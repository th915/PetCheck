package com.petcheck.backend.service.trust;

import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.entity.place.TrustGrade;
import com.petcheck.backend.entity.trust.TrustScoreHistory;
import com.petcheck.backend.repository.place.PlaceRepository;
import com.petcheck.backend.repository.trust.TrustScoreHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 청크 하나를 하나의 트랜잭션으로 처리한다.
 * <p>
 * 배치 서비스와 클래스를 나눈 이유가 있다. 같은 클래스 안에서 @Transactional 메서드를
 * 호출하면 프록시를 거치지 않아 트랜잭션이 아예 안 걸린다(자기 호출 문제).
 * 7만 건을 한 트랜잭션에 묶으면 락이 오래 잡히고 중간에 실패하면 전부 롤백되므로,
 * 청크 단위로 끊는 것이 목적인데 그러려면 빈이 분리돼 있어야 한다.
 */
@Component
@RequiredArgsConstructor
public class TrustScoreChunkProcessor {

    private final PlaceRepository placeRepository;
    private final TrustScoreHistoryRepository historyRepository;
    private final TrustScoreCalculator trustScoreCalculator;

    /**
     * @param lastId 직전 청크의 마지막 장소 id. 0 부터 시작한다
     * @return 처리 결과. processed 가 0 이면 더 볼 장소가 없다는 뜻
     */
    @Transactional
    public ChunkResult process(long lastId, int chunkSize, LocalDateTime calculatedAt) {
        List<Place> places = placeRepository
                .findBatchForRecalculation(lastId, PageRequest.of(0, chunkSize));

        if (places.isEmpty()) {
            return new ChunkResult(0, lastId, List.of());
        }

        List<Long> changedPlaceIds = new ArrayList<>();
        List<TrustScoreHistory> snapshots = new ArrayList<>(places.size());

        for (Place place : places) {
            // 리뷰를 읽지 않는다. 저장된 누적합에 오늘의 감쇠 계수만 곱한다.
            TrustGrade before = trustScoreCalculator.refresh(place);

            boolean gradeChanged = before != place.getTrustGrade();
            if (gradeChanged) {
                changedPlaceIds.add(place.getId());
            }
            snapshots.add(TrustScoreHistory.builder()
                    .place(place)
                    .gradeChanged(gradeChanged)
                    .calculatedAt(calculatedAt)
                    .build());
        }

        historyRepository.saveAll(snapshots);

        long nextCursor = places.get(places.size() - 1).getId();
        return new ChunkResult(places.size(), nextCursor, changedPlaceIds);
    }

    public record ChunkResult(int processed, long lastId, List<Long> changedPlaceIds) {
    }
}