package com.petcheck.backend.controller.admin;

import com.petcheck.backend.service.place.PlaceSyncService;
import com.petcheck.backend.service.place.SyncResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 수동 동기화.
 * <p>
 * 스케줄러로 돌리지 않는 이유: 문화정보원은 파일 데이터셋이라 갱신이 잦지 않고,
 * 개발 중에는 원할 때 눌러서 다시 밀어 넣는 편이 편하다.
 * JWT 를 붙일 때 이 경로에 hasRole("ADMIN") 을 걸어야 한다.
 */
@RestController
@RequestMapping("/api/admin/sync")
@RequiredArgsConstructor
public class PlaceSyncController {

    private final PlaceSyncService placeSyncService;

    @PostMapping("/culture")
    public SyncResult syncCultureData() {
        return placeSyncService.syncCultureData();
    }
}