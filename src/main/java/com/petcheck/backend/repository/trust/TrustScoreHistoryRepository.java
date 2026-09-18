package com.petcheck.backend.repository.trust;

import com.petcheck.backend.entity.trust.TrustScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TrustScoreHistoryRepository
        extends JpaRepository<TrustScoreHistory, Long>, TrustStatsRepository {

    /** 매장 상세 화면의 신뢰도 추이 스파크라인. */
    List<TrustScoreHistory> findAllByPlaceIdAndCalculatedAtAfterOrderByCalculatedAtAsc(
            Long placeId, LocalDateTime from);

    /**
     * "리뷰로 재조정된 장소 수".
     * 한 장소가 기간 안에 두 번 바뀌어도 1로 세야 하므로 distinct 가 필요하고,
     * 파생 쿼리 이름으로는 그 distinct 대상을 지정할 수 없어 JPQL 로 쓴다.
     */
    @Query("""
            select count(distinct h.place.id)
            from TrustScoreHistory h
            where h.gradeChanged = true
              and h.calculatedAt >= :from
            """)
    long countRegradedPlaces(@Param("from") LocalDateTime from);
}
