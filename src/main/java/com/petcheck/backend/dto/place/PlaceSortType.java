package com.petcheck.backend.dto.place;

public enum PlaceSortType {

    /** 기본값 — 신뢰도 높은 순 */
    TRUST_SCORE,

    /** 가까운 순. 좌표가 없으면 TRUST_SCORE 로 되돌아간다 */
    DISTANCE,

    /** 검증 표본이 많은 순 */
    REVIEW_COUNT,

    /** 최근에 검증된 순 */
    RECENTLY_VERIFIED
}
