package com.petcheck.backend.service.place;

import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.external.dto.PlaceSyncData;
import com.petcheck.backend.repository.place.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 청크 하나를 하나의 트랜잭션으로 처리한다.
 * <p>
 * PlaceSyncService 와 클래스를 나눈 이유: 같은 클래스 안에서 @Transactional 메서드를
 * 호출하면 프록시를 거치지 않아 트랜잭션이 아예 걸리지 않는다(자기 호출 문제).
 * <p>
 * 7만 건을 한 트랜잭션에 묶지 않는 이유도 있다. 트랜잭션이 몇 분간 열려 있으면
 * 그동안 락이 잡히고, 마지막 한 건이 실패하면 앞의 6만 건까지 전부 롤백된다.
 * 청크마다 커밋하면 중간에 죽어도 거기까지는 남는다.
 */
@Component
@RequiredArgsConstructor
public class PlaceSyncChunkProcessor {

    private final PlaceRepository placeRepository;

    @Transactional
    public SyncResult upsertChunk(List<PlaceSyncData> chunk) {
        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        for (PlaceSyncData data : chunk) {
            if (!data.isUsable()) {
                skipped++;
                continue;
            }

            Optional<Place> existing = placeRepository
                    .findBySourceTypeAndExternalId(data.sourceType(), data.externalId());

            if (existing.isPresent()) {
                // 신뢰도 누적값은 건드리지 않는다. 리뷰로 쌓은 검증 결과를 날리면 안 된다.
                existing.get().refreshFromPublicData(
                        data.name(), data.category(), data.address(), data.phone(),
                        data.latitude(), data.longitude(), data.petPolicy());
                updated++;
            } else {
                placeRepository.save(toEntity(data));
                inserted++;
            }
        }

        return new SyncResult(inserted, updated, skipped);
    }

    private Place toEntity(PlaceSyncData data) {
        return Place.builder()
                .sourceType(data.sourceType())
                .externalId(data.externalId())
                .publiclyRegistered(false)
                .name(data.name())
                .category(data.category())
                .address(data.address())
                .phone(data.phone())
                .latitude(data.latitude())
                .longitude(data.longitude())
                .imageUrl(data.imageUrl())
                .petPolicy(data.petPolicy())
                .build();
    }
}