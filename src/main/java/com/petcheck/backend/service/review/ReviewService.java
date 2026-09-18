package com.petcheck.backend.service.review;

import com.petcheck.backend.common.exception.BusinessException;
import com.petcheck.backend.common.exception.ErrorCode;
import com.petcheck.backend.common.properties.ReviewScoreProperties;
import com.petcheck.backend.common.storage.LocalImageStorage;
import com.petcheck.backend.dto.review.ReviewCreateRequest;
import com.petcheck.backend.dto.review.ReviewResponse;
import com.petcheck.backend.dto.review.ReviewUpdateRequest;
import com.petcheck.backend.dto.review.ReceiptTarget;
import com.petcheck.backend.entity.member.Member;
import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.entity.review.*;
import com.petcheck.backend.repository.member.MemberRepository;
import com.petcheck.backend.repository.place.PlaceRepository;
import com.petcheck.backend.repository.review.ReviewRepository;
import com.petcheck.backend.service.trust.TrustScoreCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository;
    private final VisitVerificationService visitVerificationService;
    private final TrustScoreCalculator trustScoreCalculator;
    private final LocalImageStorage localImageStorage;
    private final ReviewScoreProperties properties;
    private final ApplicationEventPublisher eventPublisher;

    // --- 작성 ---

    @Transactional
    public ReviewResponse create(Long memberId, ReviewCreateRequest request, String clientIp) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        Place place = placeRepository.findById(request.placeId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND));

        LocalDateTime visitedAt = request.visitedAt().atStartOfDay();

        if (reviewRepository.existsByPlaceIdAndMemberIdAndVisitedAt(place.getId(), memberId, visitedAt)) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_WRITTEN);
        }
        if (request.imageUrls() != null && request.imageUrls().size() > properties.maxImages()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "사진은 최대 " + properties.maxImages() + "장까지 첨부할 수 있습니다");
        }

        VerificationLevel level = visitVerificationService.determine(
                memberId, place, visitedAt, request.latitude(), request.longitude());

        Review review = reviewRepository.save(Review.builder()
                .place(place)
                .member(member)
                .matched(request.matched())
                .mismatchReasons(request.mismatchReasons())
                .imageUrls(request.imageUrls())
                .visitedAt(visitedAt)
                .content(request.content())
                .verification(ReviewVerification.of(level))
                .decayBase(trustScoreCalculator.decayBase(level, visitedAt))
                .writerIpHash(hashIp(clientIp))
                .receiptImageUrl(request.receiptImageUrl())
                .build());

        place.markVerified(visitedAt);

        // 영속성 컨텍스트가 비워지기 전에 응답을 만들어 둔다
        ReviewResponse response = ReviewResponse.of(
                review, trustScoreCalculator.currentWeightOf(review, LocalDateTime.now()));

        double contribution = trustScoreCalculator.contributionOf(review);
        addContribution(place.getId(), contribution, review.isMatched(), 1);
        refreshPlaceScore(place.getId());

        log.info("리뷰 작성 — reviewId={}, placeId={}, 인증={}, 기여분={}",
                review.getId(), place.getId(), level, contribution);

        eventPublisher.publishEvent(new ReviewWrittenEvent(review.getId()));

        return response;
    }

    // --- 수정 ---

    @Transactional
    public ReviewResponse update(Long memberId, Long reviewId, ReviewUpdateRequest request) {
        Review review = findOwnedReview(memberId, reviewId);
        Long placeId = review.getPlace().getId();

        // 바꾸기 전 기여분을 기억해둔다
        double before = trustScoreCalculator.contributionOf(review);
        boolean beforeMatched = review.isMatched();

        review.update(request.matched(), request.mismatchReasons(),
                request.imageUrls(), request.content());

        // 본문이 바뀌었으면 진정성이 PENDING 으로 내려가 가중치도 함께 떨어진다
        double after = trustScoreCalculator.contributionOf(review);
        boolean afterMatched = review.isMatched();

        ReviewResponse response = ReviewResponse.of(
                review, trustScoreCalculator.currentWeightOf(review, LocalDateTime.now()));

        placeRepository.addReviewContribution(placeId,
                after - before,
                (afterMatched ? after : 0.0) - (beforeMatched ? before : 0.0),
                0,
                (afterMatched ? 1 : 0) - (beforeMatched ? 1 : 0));

        refreshPlaceScore(placeId);
        eventPublisher.publishEvent(new ReviewWrittenEvent(review.getId()));

        return response;
    }

    /**
     * 진정성 판정을 반영하고 장소 신뢰도를 다시 맞춘다.
     * 가중치 w = v × d × a 에서 a 만 바뀌므로, 바뀐 만큼의 차이만 누적값에 더한다.
     * 리뷰를 다시 읽지 않기 때문에 리뷰가 몇 건이든 비용이 같다.
     */
    @Transactional
    public void applyAuthenticity(Long reviewId, AuthenticityStatus status, int ruleScore, String reason) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            return;     // 판정 도중 작성자가 지운 경우
        }

        double before = trustScoreCalculator.contributionOf(review);
        review.applyAuthenticity(ReviewAuthenticity.of(status, ruleScore, reason));
        double after = trustScoreCalculator.contributionOf(review);

        Long placeId = review.getPlace().getId();
        double delta = after - before;

        placeRepository.addReviewContribution(placeId, delta,
                review.isMatched() ? delta : 0.0, 0, 0);
        refreshPlaceScore(placeId);

        log.info("판정 반영 — reviewId={}, {} → 가중치 {} → {}", reviewId, status, before, after);
    }

    /** 영수증 대조에 필요한 값만 뽑아 준다. 리뷰가 없거나 영수증을 안 냈으면 null */
    @Transactional(readOnly = true)
    public ReceiptTarget findReceiptTarget(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(review -> new ReceiptTarget(
                        review.getId(),
                        review.getReceiptImageUrl(),
                        review.getPlace().getName(),
                        review.getVisitedAt().toLocalDate()))
                .orElse(null);
    }

    /**
     * 영수증이 확인되면 인증 등급을 RECEIPT(가중치 1.0)로 올린다.
     * NONE(0.25) 에서 올라오므로 이 리뷰의 표는 네 배가 된다.
     */
    @Transactional
    public void applyReceipt(Long reviewId, String receiptStoreName, LocalDate receiptDate) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            return;
        }

        double before = trustScoreCalculator.contributionOf(review);
        review.applyReceipt(
                ReviewVerification.byReceipt(receiptStoreName, receiptDate),
                trustScoreCalculator.decayBase(VerificationLevel.RECEIPT, review.getVisitedAt()));
        double after = trustScoreCalculator.contributionOf(review);

        Long placeId = review.getPlace().getId();
        double delta = after - before;

        placeRepository.addReviewContribution(placeId, delta,
                review.isMatched() ? delta : 0.0, 0, 0);
        refreshPlaceScore(placeId);

        log.info("영수증 인증 — reviewId={}, 상호='{}', 일자={}, 가중치 {} → {}",
                reviewId, receiptStoreName, receiptDate, before, after);
    }

    /** 관리자 모니터링 */
    public Page<ReviewResponse> findByAuthenticityStatus(AuthenticityStatus status, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();

        return reviewRepository.findAllByAuthenticityStatusOrderByCreatedAtDesc(status, pageable)
                .map(review -> ReviewResponse.of(
                        review, trustScoreCalculator.currentWeightOf(review, now)));
    }

    // --- 삭제 ---

    @Transactional
    public void delete(Long memberId, Long reviewId) {
        Review review = findOwnedReview(memberId, reviewId);

        Contribution contribution = Contribution.of(review, trustScoreCalculator);
        reviewRepository.delete(review);

        subtract(contribution);
    }

    /** 회원 탈퇴 시 호출. 각 장소의 누적값에서 기여분을 빼고 점수를 다시 맞춘다 */
    @Transactional
    public void deleteAllByMember(Long memberId) {
        List<Review> reviews = reviewRepository.findAllByMemberId(memberId);
        if (reviews.isEmpty()) {
            return;
        }

        // 엔티티가 영속 상태일 때 필요한 값을 미리 뽑는다.
        // 벌크 UPDATE 가 컨텍스트를 비우면 지연 로딩이 불가능해진다.
        List<Contribution> contributions = reviews.stream()
                .map(review -> Contribution.of(review, trustScoreCalculator))
                .toList();

        reviewRepository.deleteAll(reviews);
        contributions.forEach(this::subtract);
    }

    // --- 조회 ---

    public Page<ReviewResponse> findByPlace(Long placeId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();

        // 조작으로 판정된 리뷰는 목록에도 내리지 않는다
        return reviewRepository
                .findAllByPlaceIdAndAuthenticityStatusNotOrderByCreatedAtDesc(
                        placeId, AuthenticityStatus.FAKE, pageable)
                .map(review -> ReviewResponse.of(
                        review, trustScoreCalculator.currentWeightOf(review, now)));
    }

    public Page<ReviewResponse> findMyReviews(Long memberId, Pageable pageable) {
        LocalDateTime now = LocalDateTime.now();

        return reviewRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId, pageable)
                .map(review -> ReviewResponse.of(
                        review, trustScoreCalculator.currentWeightOf(review, now)));
    }

    public String uploadImage(MultipartFile file) {
        return localImageStorage.save(file);
    }

    // --- 내부 ---

    /** 삭제 전에 뽑아두는 기여분 스냅샷 */
    private record Contribution(Long placeId, double amount, boolean matched) {

        static Contribution of(Review review, TrustScoreCalculator calculator) {
            return new Contribution(
                    review.getPlace().getId(),
                    calculator.contributionOf(review),
                    review.isMatched());
        }
    }

    private void subtract(Contribution contribution) {
        addContribution(contribution.placeId(), -contribution.amount(), contribution.matched(), -1);
        refreshPlaceScore(contribution.placeId());
    }

    private void addContribution(Long placeId, double amount, boolean matched, int countDelta) {
        placeRepository.addReviewContribution(placeId,
                amount,
                matched ? amount : 0.0,
                countDelta,
                matched ? countDelta : 0);
    }

    /** 누적값이 바뀌었으니 점수를 다시 맞춘다. store 를 함께 읽어야 사전 점수가 정확하다 */
    private void refreshPlaceScore(Long placeId) {
        placeRepository.findDetailById(placeId).ifPresent(trustScoreCalculator::refresh);
    }

    private Review findOwnedReview(Long memberId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.NOT_REVIEW_OWNER);
        }
        return review;
    }

    /**
     * IP 원본은 남기지 않는다. 동일 단말에서 여러 계정이 도배하는 패턴만 잡으면 되므로
     * 해시로 충분하고, 그 이상은 보관할 이유가 없다.
     */
    private String hashIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(ip.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}