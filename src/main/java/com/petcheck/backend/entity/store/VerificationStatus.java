package com.petcheck.backend.entity.store;

public enum VerificationStatus {

    /**
     * 매장 등록은 됐지만 법정 기준 체크리스트가 전부 충족되지 않아
     * 인증을 신청하지 않은 상태. 관리자 승인 대기열(PENDING)에 올라가지 않는다.
     */
    NOT_REQUESTED,

    /** 체크리스트 8개 전부 충족 — 인증 신청이 접수되어 관리자 승인 대기 중 */
    PENDING,

    /** 관리자 승인 — 이때부터 법정 기준 인증 배지가 노출된다 */
    APPROVED,

    /** 반려 — rejectReason 에 사유가 남는다 */
    REJECTED
}
