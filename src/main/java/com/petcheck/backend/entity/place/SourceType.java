package com.petcheck.backend.entity.place;

/**
 * 장소 정보의 출처. 신뢰도 사전 확률(m)의 출발점이자
 * "출처별 정확도 비교" 지표의 분류 축이다.
 */
public enum SourceType {

    /** 한국관광공사 TourAPI 4.0 — 반려동물 동반여행정보 */
    TOUR_API,

    /** 한국문화정보원 — 전국 반려동물 동반 가능 문화시설 위치 데이터 */
    CULTURE_DATA,

    /** 식품안전나라 — 반려동물 동반 가능 업소 현황 */
    FOOD_SAFETY,

    /** 매장이 직접 등록 — 공공데이터에 없던 장소 */
    STORE_SUBMITTED
}
