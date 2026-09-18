package com.petcheck.backend.entity.member;

public enum MemberRole {

    /** 일반 사용자 — 검색, 리뷰 작성, 반려동물 프로필, 캘린더 */
    CUSTOMER,

    /** 매장 사장님 — 매장 등록, 법정기준 체크리스트 입력 */
    STORE,

    /** 관리자 — 매장 등록 승인, 정확도 통계 대시보드 */
    ADMIN
}
