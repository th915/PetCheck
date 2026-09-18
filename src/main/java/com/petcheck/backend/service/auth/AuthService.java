package com.petcheck.backend.service.auth;

import com.petcheck.backend.common.exception.BusinessException;
import com.petcheck.backend.common.exception.ErrorCode;
import com.petcheck.backend.common.security.JwtTokenProvider;
import com.petcheck.backend.dto.auth.LoginRequest;
import com.petcheck.backend.dto.auth.MemberResponse;
import com.petcheck.backend.dto.auth.SignUpRequest;
import com.petcheck.backend.dto.auth.TokenResponse;
import com.petcheck.backend.entity.member.Member;
import com.petcheck.backend.entity.member.MemberRole;
import com.petcheck.backend.repository.member.MemberRepository;
import com.petcheck.backend.service.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.petcheck.backend.repository.favorite.FavoriteRepository;
import com.petcheck.backend.repository.pet.PetProfileRepository;
import com.petcheck.backend.repository.review.ReviewRepository;
import com.petcheck.backend.repository.schedule.VisitScheduleRepository;
import com.petcheck.backend.repository.store.StoreRepository;
import com.petcheck.backend.service.store.StoreService;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    private final StoreService storeService;
    private final ReviewService reviewService;                 // ReviewRepository 대신
    private final PetProfileRepository petProfileRepository;
    private final FavoriteRepository favoriteRepository;
    private final VisitScheduleRepository visitScheduleRepository;

    @Transactional
    public MemberResponse signUp(SignUpRequest request) {
        if (request.role() == MemberRole.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    "관리자 계정은 가입으로 만들 수 없습니다");
        }
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = memberRepository.save(Member.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .nickname(request.nickname())
                .role(request.role())
                .build());

        return MemberResponse.from(member);
    }

    public TokenResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        return TokenResponse.of(
                tokenProvider.createToken(member), tokenProvider.validity(), member);
    }

    public MemberResponse findById(Long memberId) {
        return memberRepository.findById(memberId)
                .map(MemberResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 회원 탈퇴. 자식 데이터를 순서대로 정리한 뒤 회원을 지운다.
     * <p>
     * 외래키 때문에 순서가 정해져 있다. visit_schedule 이 pet_profile 을 참조하므로
     * 일정을 먼저 지워야 하고, store 는 place 의 신뢰도를 되돌려야 해서 전용 메서드를 쓴다.
     * <p>
     * 관리자는 탈퇴할 수 없다. 지우면 승인·동기화를 할 계정이 사라진다.
     */
    @Transactional
    public void withdraw(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        if (member.getRole() == MemberRole.ADMIN) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "관리자 계정은 탈퇴할 수 없습니다");
        }

        // 리뷰를 지우면서 각 장소의 신뢰도 누적값도 함께 되돌린다
        reviewService.deleteAllByMember(memberId);
        visitScheduleRepository.deleteByMemberId(memberId);
        petProfileRepository.deleteByMemberId(memberId);
        favoriteRepository.deleteByMemberId(memberId);

        // 매장이 여러 개일 수 있다. 각각 지우면서 해당 장소의 신뢰도가 0.5 로 돌아간다
        storeService.deleteAllByOwner(memberId);

        memberRepository.delete(member);
    }

}