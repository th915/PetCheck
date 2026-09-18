package com.petcheck.backend.repository.trust;

import com.petcheck.backend.dto.trust.SourceAccuracy;
import com.petcheck.backend.dto.trust.TrustTrendPoint;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 관리자 정확도 통계 대시보드의 집계 쿼리.
 * TrustScoreHistoryRepository 가 이 인터페이스를 상속하므로
 * 구현체 이름은 TrustStatsRepositoryImpl 이어야 한다.
 */
public interface TrustStatsRepository {

    /** 전체 장소의 평균 신뢰도 */
    Double averageTrustScore();

    /** 출처별 정확도 비교 — 매장 등록 장소 vs 공공데이터만 있는 장소 */
    List<SourceAccuracy> compareAccuracyBySource();

    /** 평균 신뢰도 추이 (일 단위) */
    List<TrustTrendPoint> trendSince(LocalDateTime from);
}
