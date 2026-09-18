package com.petcheck.backend.dto.trust;

/**
 * 평균 신뢰도 추이 차트의 점 하나.
 *
 * @param date         yyyy-MM-dd
 * @param averageScore 그날 재계산된 장소들의 평균 trust_score
 */
public record TrustTrendPoint(String date, Double averageScore) {
}
