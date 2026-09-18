package com.petcheck.backend.entity.review;

public enum AuthenticityStatus {

    /** AI 분석 대기. 가중치 0.5 로 절반만 반영된다 */
    PENDING,

    /** 정상 리뷰 */
    GENUINE,

    /** 의심 신호가 있으나 확정은 아님 */
    SUSPICIOUS,

    /** 조작으로 판정. 가중치 0 이라 분모에서도 빠져 존재하지 않는 리뷰가 된다 */
    FAKE
}