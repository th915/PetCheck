package com.petcheck.backend.service.place;

import com.petcheck.backend.external.culture.CultureFacilityCsvReader;
import com.petcheck.backend.external.dto.PlaceSyncData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 공공데이터를 Place 로 밀어 넣는다.
 * <p>
 * 이 클래스에는 @Transactional 이 없다. 청크 하나가 트랜잭션 하나이고,
 * 여기는 읽어온 목록을 잘라서 순서대로 넘기는 역할만 한다.
 * <p>
 * 출처가 늘어나도 이 서비스는 그대로다. PlaceSyncData 리스트만 받으면 되므로
 * 새 기관이 생기면 "그 기관 → PlaceSyncData" 변환기 하나만 추가하면 된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceSyncService {

    private static final int CHUNK_SIZE = 500;

    private final CultureFacilityCsvReader cultureFacilityCsvReader;
    private final PlaceSyncChunkProcessor chunkProcessor;

    public SyncResult syncCultureData() {
        List<PlaceSyncData> rows = cultureFacilityCsvReader.read();
        log.info("문화정보원 동기화 시작 — {}건", rows.size());

        return upsertAll(rows);
    }

    public SyncResult upsertAll(List<PlaceSyncData> rows) {
        SyncResult total = SyncResult.empty();

        for (int start = 0; start < rows.size(); start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, rows.size());

            // subList 는 복사본이 아니라 뷰다. 읽기만 하므로 그대로 넘겨도 된다.
            total = total.plus(chunkProcessor.upsertChunk(rows.subList(start, end)));

            log.info("동기화 진행 — {}/{} (신규 {}, 갱신 {})",
                    end, rows.size(), total.inserted(), total.updated());
        }

        log.info("동기화 완료 — 신규 {}, 갱신 {}, 제외 {}",
                total.inserted(), total.updated(), total.skipped());

        return total;
    }
}