package com.petcheck.backend.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 신뢰도 공식의 가중치 상수 C.
 * <p>
 * 코드에 박지 않고 설정으로 뺀 이유: C 를 얼마로 잡을지는 정답이 없고
 * 실제 리뷰가 쌓이면 조정하게 된다(제안서 "학습·고민 포인트" 1번).
 * 설정값이면 재빌드 없이 바꿔가며 결과를 비교할 수 있고,
 * 그 비교 기록이 발표 자료의 근거가 된다.
 * <p>
 * C = 5 의 의미: 리뷰 5건에서 사전 점수와 실측값의 영향력이 반반이 된다.
 *
 * @param weight 사전 확률 가중치 C
 */
@ConfigurationProperties(prefix = "app.trust")
public record TrustScoreProperties(double weight) {

    public TrustScoreProperties {
        if (weight <= 0) {
            throw new IllegalArgumentException("app.trust.weight 는 0보다 커야 합니다: " + weight);
        }
    }
}
