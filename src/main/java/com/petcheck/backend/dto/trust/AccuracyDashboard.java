package com.petcheck.backend.dto.trust;

import java.util.List;

/**
 * 관리자 정확도 통계 대시보드 한 화면분.
 * 제안서의 지표 카드 4개가 그대로 필드가 된다.
 *
 * @param averageTrustScore    전체 장소의 평균 신뢰도
 * @param legalCertifiedRatio  등록 매장 중 법정 시설기준을 충족한 비율 (0.0 ~ 1.0)
 * @param regradedPlaceCount   최근 기간 안에 리뷰로 등급이 바뀐 장소 수
 * @param sourceAccuracies     출처별 정확도 비교 (2줄)
 * @param trend                평균 신뢰도 추이
 */
public record AccuracyDashboard(
        Double averageTrustScore,
        Double legalCertifiedRatio,
        Long regradedPlaceCount,
        List<SourceAccuracy> sourceAccuracies,
        List<TrustTrendPoint> trend
) {
}
