package com.petcheck.backend.common.properties;

import com.petcheck.backend.entity.review.AuthenticityStatus;
import com.petcheck.backend.entity.review.VerificationLevel;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * 리뷰 가중치 파라미터. 전부 설정으로 뺀 이유는 정답이 없어서다.
 * 실제 리뷰가 쌓이면 값을 바꿔가며 비교하게 되고, 그 비교 기록이 발표 근거가 된다.
 * <p>
 * halfLifeDays 나 verificationWeights 를 바꾸면 저장된 decaySum 이 전부 무효가 된다.
 * 그때는 POST /api/admin/trust/rebuild 로 재구축해야 한다.
 */
@ConfigurationProperties(prefix = "app.review")
public record ReviewScoreProperties(
        int halfLifeDays,
        double minWeight,
        int maxImages,
        double locationRadiusMeters,
        Map<VerificationLevel, Double> verificationWeights,
        Map<AuthenticityStatus, Double> authenticityWeights
) {

    public ReviewScoreProperties {
        if (halfLifeDays <= 0) {
            throw new IllegalArgumentException("app.review.half-life-days 는 0보다 커야 합니다");
        }
    }

    public double verificationWeight(VerificationLevel level) {
        return verificationWeights.getOrDefault(level, 0.0);
    }

    public double authenticityWeight(AuthenticityStatus status) {
        return authenticityWeights.getOrDefault(status, 0.0);
    }
}