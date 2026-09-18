package com.petcheck.backend.service.review;

import com.petcheck.backend.dto.review.ReviewInspection;
import com.petcheck.backend.entity.review.Review;
import com.petcheck.backend.repository.review.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * LLM 을 부르기 전에 돌리는 규칙 검사.
 * <p>
 * 목적이 두 가지다. 하나는 명백한 조작을 공짜로 걸러 Gemini 호출 비용을 아끼는 것,
 * 다른 하나는 "왜 가짜로 판정했는가"를 LLM 판단에만 의존하지 않고 설명하는 것이다.
 * 심사에서 근거를 요구받을 때 규칙 이름과 점수를 그대로 보여줄 수 있다.
 */
@Component
@RequiredArgsConstructor
public class RuleBasedSpamFilter {

    // 전화번호, 카톡/텔레그램 아이디, 외부 링크 — 리뷰가 아니라 영업이다
    private static final Pattern CONTACT = Pattern.compile(
            "01[016-9][-. ]?\\d{3,4}[-. ]?\\d{4}|카톡|카카오톡|오픈채팅|텔레그램|https?://");

    // 대가성 표시. 표시했다는 것 자체는 정직하지만, 매장 신뢰도 근거로는 쓸 수 없다
    private static final Pattern SPONSORED = Pattern.compile(
            "체험단|협찬|제공받|제공 받|원고료|대가를 받|소정의|무상 제공");

    private static final Pattern PROMO = Pattern.compile(
            "예약\\s*문의|문의\\s*주세요|할인\\s*코드|쿠폰\\s*코드|이벤트\\s*참여|최저가");

    // 같은 글자 5번 이상 — "ㅋㅋㅋㅋㅋ", "!!!!!"
    private static final Pattern REPEATED = Pattern.compile("(.)\\1{4,}");

    private static final int BURST_MINUTES = 10;
    private static final int BURST_LIMIT = 3;
    private static final int MULTI_ACCOUNT_HOURS = 24;
    private static final int MULTI_ACCOUNT_LIMIT = 3;
    private static final int NEW_ACCOUNT_MINUTES = 5;

    private final ReviewRepository reviewRepository;

    /**
     * 판정에 필요한 값을 트랜잭션 안에서 전부 뽑아 반환한다.
     * 판정 중 리뷰가 지워졌으면 null 을 준다.
     */
    @Transactional(readOnly = true)
    public ReviewInspection inspect(Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            return null;
        }

        String content = review.getContent() == null ? "" : review.getContent();
        LocalDateTime createdAt = review.getCreatedAt();
        Long memberId = review.getMember().getId();
        Long placeId = review.getPlace().getId();

        List<String> hits = new ArrayList<>();
        int score = 0;

        // ① 연락처·외부링크 노출
        if (CONTACT.matcher(content).find()) {
            score += 40;
            hits.add("연락처 또는 외부 링크 포함(40)");
        }

        // ② 대가성 리뷰 표시
        if (SPONSORED.matcher(content).find()) {
            score += 40;
            hits.add("체험단·협찬 문구(40)");
        }

        // ③ 홍보성 문구
        if (PROMO.matcher(content).find()) {
            score += 20;
            hits.add("홍보성 문구(20)");
        }

        // ④ 단시간 도배
        long burst = reviewRepository.countByMemberIdAndCreatedAtAfter(
                memberId, createdAt.minusMinutes(BURST_MINUTES));
        if (burst >= BURST_LIMIT) {
            score += 30;
            hits.add(BURST_MINUTES + "분 내 " + burst + "건 연속 작성(30)");
        }

        // ⑤ 같은 단말에서 여러 계정이 한 장소를 집중 공략
        if (review.getWriterIpHash() != null) {
            long writers = reviewRepository.countDistinctWritersByIp(
                    review.getWriterIpHash(), placeId, createdAt.minusHours(MULTI_ACCOUNT_HOURS));
            if (writers >= MULTI_ACCOUNT_LIMIT) {
                score += 40;
                hits.add("동일 단말에서 " + writers + "개 계정이 같은 장소에 작성(40)");
            }
        }

        // ⑥ 가입 직후 작성 — 리뷰를 쓰려고 만든 계정
        if (review.getMember().getCreatedAt() != null
                && review.getMember().getCreatedAt().plusMinutes(NEW_ACCOUNT_MINUTES).isAfter(createdAt)) {
            score += 20;
            hits.add("가입 " + NEW_ACCOUNT_MINUTES + "분 이내 작성(20)");
        }

        // ⑦ 같은 본문 복붙
        if (!content.isBlank() && reviewRepository.countByMemberIdAndContent(memberId, content) > 1) {
            score += 30;
            hits.add("동일 본문 재사용(30)");
        }

        // ⑧ 문자 도배
        if (REPEATED.matcher(content).find()) {
            score += 10;
            hits.add("동일 문자 반복(10)");
        }

        // ⑨ 불일치라면서 사유를 하나도 고르지 않음 — 내용과 선택이 어긋난다
        if (!review.isMatched() && review.getMismatchReasons().isEmpty()) {
            score += 10;
            hits.add("불일치 선택했으나 사유 미기재(10)");
        }

        return new ReviewInspection(
                reviewId,
                score,
                hits.isEmpty() ? "규칙 위반 없음" : String.join(", ", hits),
                content,
                review.isMatched(),
                review.getPlace().getName());
    }
}