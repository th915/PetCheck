package com.petcheck.backend.service.trust;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 전체 장소의 신뢰도를 다시 계산한다.
 * <p>
 * 이 클래스에는 @Transactional 이 없다. 청크 하나가 트랜잭션 하나이고,
 * 여기는 청크를 순서대로 돌리는 역할만 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrustScoreBatchService {

    private static final int CHUNK_SIZE = 500;

    private final TrustScoreChunkProcessor chunkProcessor;

    public BatchResult recalculateAll() {
        // 이번 실행의 모든 스냅샷이 같은 시각을 공유해야 일자별 집계가 한 덩어리로 묶인다.
        LocalDateTime calculatedAt = LocalDateTime.now();

        List<Long> changedPlaceIds = new ArrayList<>();
        long lastId = 0;
        int processed = 0;

        log.info("신뢰도 재계산 시작");

        while (true) {
            TrustScoreChunkProcessor.ChunkResult chunk =
                    chunkProcessor.process(lastId, CHUNK_SIZE, calculatedAt);

            if (chunk.processed() == 0) {
                break;
            }

            processed += chunk.processed();
            lastId = chunk.lastId();
            changedPlaceIds.addAll(chunk.changedPlaceIds());

            log.info("신뢰도 재계산 진행 — {}건 (마지막 id={})", processed, lastId);
        }

        log.info("신뢰도 재계산 완료 — {}건 처리, {}건 등급 변경", processed, changedPlaceIds.size());

        return new BatchResult(processed, changedPlaceIds.size(), changedPlaceIds, calculatedAt);
    }
}