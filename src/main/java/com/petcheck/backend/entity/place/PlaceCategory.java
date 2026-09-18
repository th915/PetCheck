package com.petcheck.backend.entity.place;

import java.util.EnumSet;
import java.util.Set;

/**
 * 지도 카테고리 필터의 기준.
 * <p>
 * 세 묶음으로 나뉜다.
 * - VISIT : 동반 출입 조건이 쟁점인 곳. 펫체크의 본래 대상이다
 * - CARE  : 맡기는 곳
 * - INFRA : 동반이 당연해서 출입 조건이 쟁점이 아닌 곳.
 *           문화정보원 데이터의 95%가 여기다. 버리지 않고 "여행 중 응급" 용도로 쓴다
 * <p>
 * 제안서에서는 storeType 이라 불렀지만 Store 가 아니라 Place 에 둔다.
 * 매장 등록이 없는 공공데이터 장소도 카테고리 필터에 걸려야 하기 때문이다.
 */
public enum PlaceCategory {

    // 동반 출입 대상
    RESTAURANT,
    CAFE,
    ACCOMMODATION,
    CULTURE,
    TOURIST_SPOT,

    // 맡기는 곳
    PET_SITTER,
    PET_HOTEL,
    PET_KINDERGARTEN,

    // 반려 인프라
    VET,
    PET_PHARMACY,
    PET_SUPPLY,
    PET_GROOMING,

    ETC;

    private static final Set<PlaceCategory> VISIT =
            EnumSet.of(RESTAURANT, CAFE, ACCOMMODATION, CULTURE, TOURIST_SPOT);

    private static final Set<PlaceCategory> CARE =
            EnumSet.of(PET_SITTER, PET_HOTEL, PET_KINDERGARTEN);

    private static final Set<PlaceCategory> INFRA =
            EnumSet.of(VET, PET_PHARMACY, PET_SUPPLY, PET_GROOMING);

    /** 검색·지도의 기본 대상. 프론트가 파라미터를 안 주면 이 묶음을 쓴다 */
    public static Set<PlaceCategory> visitCategories() {
        return EnumSet.copyOf(VISIT);
    }

    public static Set<PlaceCategory> careFacilities() {
        return EnumSet.copyOf(CARE);
    }

    public static Set<PlaceCategory> infraCategories() {
        return EnumSet.copyOf(INFRA);
    }

    public boolean isCareFacility() {
        return CARE.contains(this);
    }
}