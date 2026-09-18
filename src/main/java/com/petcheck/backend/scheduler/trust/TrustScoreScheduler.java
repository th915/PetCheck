package com.petcheck.backend.scheduler.trust;

import com.petcheck.backend.service.trust.BatchResult;
import com.petcheck.backend.service.trust.TrustScoreBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 신뢰도 재계산을 주기적으로 돌린다.
 * <p>
 * 스케줄러는 "언제 부를지"만 안다. 무엇을 하는지는 서비스가 갖는다.
 * 이렇게 나눠야 관리자가 버튼으로 수동 실행하는 경로와 같은 코드를 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrustScoreScheduler {

    private final TrustScoreBatchService trustScoreBatchService;

    @Scheduled(cron = "${app.trust.recalculation-cron}")
    public void recalculate() {
        BatchResult result = trustScoreBatchService.recalculateAll();

        // TODO: result.changedPlaceIds() 로 FavoriteRepository.findMemberIdsByPlaceIds() 를 부르고
        //       SSE 로 알림을 보낸다. 알림 파이프라인 작업 때 채운다.
        if (result.gradeChanged() > 0) {
            log.info("등급이 바뀐 장소 {}건 — 알림 대상", result.gradeChanged());
        }
    }
}