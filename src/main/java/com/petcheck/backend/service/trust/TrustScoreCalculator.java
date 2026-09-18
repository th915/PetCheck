package com.petcheck.backend.service.trust;

import com.petcheck.backend.common.properties.ReviewScoreProperties;
import com.petcheck.backend.common.properties.TrustScoreProperties;
import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.entity.place.TrustGrade;
import com.petcheck.backend.entity.review.AuthenticityStatus;
import com.petcheck.backend.entity.review.Review;
import com.petcheck.backend.entity.review.VerificationLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 신뢰도 공식의 모든 계산이 여기 모인다.
 * <pre>
 *   trust_score = (C·m + Σ wᵢ·matchᵢ) / (C + Σ wᵢ)
 *   wᵢ = v(인증) × d(시간감쇠) × a(진정성)
 * </pre>
 * 지수 감쇠는 분리된다 — d = 2^(−t/H) × 2^(tᵢ/H) 이므로
 * 오른쪽 항만 리뷰에 저장해두면 합을 증분으로 유지할 수 있다.
 */
@Component
@RequiredArgsConstructor
public class TrustScoreCalculator {

    /**
     * 감쇠 계산의 기준 시각. 서비스 시작일로 잡는다.
     * <p>
     * Unix epoch 를 쓰면 2^(56년/180일) 이 되어 수가 지나치게 커진다.
     * 이 값을 바꾸면 저장된 decayBase 가 전부 무효가 되므로 절대 수정하지 않는다.
     */
    private static final LocalDateTime DECAY_EPOCH = LocalDateTime.of(2026, 1, 1, 0, 0);

    private final TrustScoreProperties trustScoreProperties;
    private final ReviewScoreProperties reviewScoreProperties;

    /**
     * 리뷰의 시간 독립 가중치. 생성 시 한 번 계산해 저장한다.
     * v × 2^(방문일/H)
     */
    public double decayBase(VerificationLevel level, LocalDateTime visitedAt) {
        return reviewScoreProperties.verificationWeight(level)
                * Math.pow(2, daysFromEpoch(visitedAt) / reviewScoreProperties.halfLifeDays());
    }

    /** 조회 시점의 감쇠 계수. 2^(−지금/H) */
    public double decayFactor(LocalDateTime at) {
        return Math.pow(2, -daysFromEpoch(at) / reviewScoreProperties.halfLifeDays());
    }

    public double authenticityWeight(AuthenticityStatus status) {
        return reviewScoreProperties.authenticityWeight(status);
    }

    /** 이 리뷰가 Place 누적합에 더할 값. 삭제할 때는 이 값을 빼면 된다 */
    public double contributionOf(Review review) {
        return review.getDecayBase() * authenticityWeight(review.getAuthenticity().getStatus());
    }

    /** 지금 시점에서 이 리뷰가 실제로 몇 표인지. 화면 표시와 시연용 */
    public double currentWeightOf(Review review, LocalDateTime at) {
        return contributionOf(review) * decayFactor(at);
    }

    /** 저장된 누적합으로 점수를 다시 계산한다. 리뷰를 읽지 않는다 */
    public TrustGrade refresh(Place place) {
        return place.refreshTrustScore(trustScoreProperties.weight(), decayFactor(LocalDateTime.now()));
    }

    /** 소수점까지 세야 하루 미만 차이도 반영된다 */
    private double daysFromEpoch(LocalDateTime at) {
        return Duration.between(DECAY_EPOCH, at).toMinutes() / (60.0 * 24.0);
    }
}