package com.petcheck.backend.entity.store;

public enum VerificationStatus {

    /** 등록 제출 완료, 관리자 승인 대기 */
    PENDING,

    /** 관리자 승인 — 이때부터 법정 기준 인증 배지가 노출된다 */
    APPROVED,

    /** 반려 — rejectReason 에 사유가 남는다 */
    REJECTED
}
