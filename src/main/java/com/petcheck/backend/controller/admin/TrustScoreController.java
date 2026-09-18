package com.petcheck.backend.controller.admin;

import com.petcheck.backend.service.trust.BatchResult;
import com.petcheck.backend.service.trust.TrustScoreBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 수동 재계산.
 * 스케줄을 기다리지 않고 지금 돌려볼 수 있어야 개발이 된다.
 * JWT 를 붙일 때 이 경로에 hasRole("ADMIN") 을 건다.
 */
@RestController
@RequestMapping("/api/admin/trust")
@RequiredArgsConstructor
public class TrustScoreController {

    private final TrustScoreBatchService trustScoreBatchService;

    @PostMapping("/recalculate")
    public BatchResult recalculate() {
        return trustScoreBatchService.recalculateAll();
    }
}