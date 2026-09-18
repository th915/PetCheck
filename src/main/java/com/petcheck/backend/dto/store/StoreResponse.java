package com.petcheck.backend.dto.store;

import com.petcheck.backend.entity.store.LegalChecklist;
import com.petcheck.backend.entity.store.Store;
import com.petcheck.backend.entity.store.VerificationStatus;

import java.time.LocalDateTime;

/**
 * @param satisfiedCount 8개 중 몇 개를 충족했는지. 화면에서 "6/8" 로 쓴다.
 *                       LegalChecklist 안의 satisfiedCount() 는 getter 이름 규칙에
 *                       맞지 않아 JSON 에 안 나오므로 여기서 꺼내 준다
 */
public record StoreResponse(
        Long id,
        Long placeId,
        String placeName,
        String businessName,
        String businessNumber,
        LegalChecklist legalChecklist,
        int satisfiedCount,
        String proofImageUrl,
        VerificationStatus verificationStatus,
        boolean legalCertified,
        String rejectReason,
        LocalDateTime approvedAt
) {

    public static StoreResponse from(Store store) {
        LegalChecklist checklist = store.getLegalChecklist();

        return new StoreResponse(
                store.getId(),
                store.getPlace().getId(),
                store.getPlace().getName(),
                store.getBusinessName(),
                store.getBusinessNumber(),
                checklist,
                checklist == null ? 0 : checklist.satisfiedCount(),
                store.getProofImageUrl(),
                store.getVerificationStatus(),
                store.isLegalCertified(),
                store.getRejectReason(),
                store.getApprovedAt());
    }
}