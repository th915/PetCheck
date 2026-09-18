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
    STORE_SUBMITTED,

    /**
     * 카카오맵 검색 결과를 이용자가 즐겨찾기·일정·리뷰에 쓰면서 그 순간 만들어짐.
     * 공공데이터 4종에 없는 곳도 검색·이용은 되게 하되, 동반 조건은 전혀 모르므로
     * petPolicy 는 비워 두고 리뷰로 채워나간다.
     */
    KAKAO_API
}
