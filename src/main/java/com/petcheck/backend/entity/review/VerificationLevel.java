package com.petcheck.backend.entity.review;

/**
 * 방문을 얼마나 확실하게 증명했는가.
 * 위쪽일수록 조작이 어렵고 가중치가 높다.
 * 여러 조건을 만족하면 가장 높은 등급을 채택한다.
 */
public enum VerificationLevel {

    /** 영수증 사진에서 상호명·결제일을 읽어 매장과 대조 통과 */
    RECEIPT,

    /** 첨부 사진의 EXIF GPS 가 장소 반경 안이고 촬영일이 방문일과 일치 */
    PHOTO_EXIF,

    /** 리뷰 작성 시점의 브라우저 좌표가 장소 반경 안 */
    LOCATION,

    /** 방문 전에 캘린더에 일정을 등록해뒀음 */
    SCHEDULE,

    /** 근거 없음 */
    NONE
}