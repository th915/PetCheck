package com.petcheck.backend.entity.review;

/**
 * "실제와 달랐다"고 답했을 때 무엇이 달랐는지.
 * 자유 서술만 받으면 어느 정보가 틀렸는지 집계할 수 없어 열거형으로 받는다.
 * 이 분포가 관리자 대시보드의 "어떤 정보가 가장 자주 틀리는가"가 된다.
 */
public enum MismatchReason {

    /** 동반 자체가 불가했다 */
    NOT_ALLOWED,

    /** 크기·체중 제한이 등록 정보와 달랐다 */
    SIZE_LIMIT_DIFFERENT,

    /** 실내 불가 등 구역 조건이 달랐다 */
    AREA_CONDITION_DIFFERENT,

    /** 이동장·목줄 등 요구 조건이 달랐다 */
    REQUIREMENT_DIFFERENT,

    /** 추가 요금이 달랐다 */
    FEE_DIFFERENT,

    /** 폐업했거나 영업하지 않았다 */
    CLOSED,

    OTHER
}
