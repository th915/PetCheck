package com.petcheck.backend.service.trust;

import com.petcheck.backend.dto.review.MismatchReasonCount;
import com.petcheck.backend.dto.trust.AccuracyDashboard;
import com.petcheck.backend.repository.review.ReviewRepository;
import com.petcheck.backend.repository.store.StoreRepository;
import com.petcheck.backend.repository.trust.TrustScoreHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 제안서의 지표 카드 4개를 한 번에 만든다.
 * "정확도를 높이기 위해 무엇을 했는가"를 숫자로 보여주는 화면의 데이터 소스다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccuracyDashboardService {

    private final TrustScoreHistoryRepository historyRepository;
    private final StoreRepository storeRepository;
    private final ReviewRepository reviewRepository;

    public AccuracyDashboard getDashboard(int days) {
        LocalDateTime from = LocalDateTime.now().minusDays(days);

        return new AccuracyDashboard(
                historyRepository.averageTrustScore(),
                legalCertifiedRatio(),
                historyRepository.countRegradedPlaces(from),
                historyRepository.compareAccuracyBySource(),
                historyRepository.trendSince(from)
        );
    }

    /** "어떤 정보가 가장 자주 틀리는가" — 개선 우선순위를 정하는 근거 */
    public List<MismatchReasonCount> mismatchReasons(int days) {
        return reviewRepository.countMismatchReasons(LocalDateTime.now().minusDays(days));
    }

    /**
     * 등록 매장 중 법정 시설기준을 충족한 비율.
     * 등록 매장이 한 곳도 없으면 0 이 아니라 null 이다.
     * 0% 와 "아직 표본이 없음"은 화면에서 다르게 보여야 한다.
     */
    private Double legalCertifiedRatio() {
        long total = storeRepository.count();
        if (total == 0) {
            return null;
        }
        return (double) storeRepository.countByLegalCertifiedTrue() / total;
    }
}