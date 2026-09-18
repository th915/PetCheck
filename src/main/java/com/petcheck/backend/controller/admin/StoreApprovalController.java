package com.petcheck.backend.controller.admin;

import com.petcheck.backend.dto.common.PageResponse;
import com.petcheck.backend.dto.store.StoreRejectRequest;
import com.petcheck.backend.dto.store.StoreResponse;
import com.petcheck.backend.entity.store.VerificationStatus;
import com.petcheck.backend.service.store.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
public class StoreApprovalController {

    private final StoreService storeService;

    /** 승인 대기 목록. 기본값이 PENDING 이라 관리자가 바로 심사할 것만 본다 */
    @GetMapping
    public PageResponse<StoreResponse> list(
            @RequestParam(defaultValue = "PENDING") VerificationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {

        return PageResponse.of(storeService.findByStatus(status, pageable));
    }

    @PostMapping("/{id}/approve")
    public StoreResponse approve(@PathVariable Long id) {
        return storeService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public StoreResponse reject(@PathVariable Long id,
                                @Valid @RequestBody StoreRejectRequest request) {
        return storeService.reject(id, request.reason());
    }
}