package com.petcheck.backend.external.support;

import com.petcheck.backend.entity.place.PlaceCategory;

import java.util.Map;

/**
 * 문화정보원 카테고리 문자열을 PlaceCategory 로 옮긴다.
 * <p>
 * 카테고리3 의 값이 13종으로 확정돼 있어 키워드 매칭이 아니라 정확 매핑을 쓴다.
 * 값이 늘어나면 ETC 로 떨어지고, 그때 이 표에 한 줄 추가하면 된다.
 * 추측으로 CULTURE 에 몰아넣지 않는 것이 중요하다 — 그러면 잘못 분류된 걸 알아챌 수 없다.
 */
public final class CategoryMapper {

    private static final Map<String, PlaceCategory> BY_CATEGORY3 = Map.ofEntries(
            Map.entry("동물약국", PlaceCategory.PET_PHARMACY),
            Map.entry("동물병원", PlaceCategory.VET),
            Map.entry("반려동물용품", PlaceCategory.PET_SUPPLY),
            Map.entry("미용", PlaceCategory.PET_GROOMING),
            Map.entry("위탁관리", PlaceCategory.PET_SITTER),

            Map.entry("카페", PlaceCategory.CAFE),
            Map.entry("식당", PlaceCategory.RESTAURANT),
            Map.entry("펜션", PlaceCategory.ACCOMMODATION),
            Map.entry("호텔", PlaceCategory.ACCOMMODATION),

            Map.entry("여행지", PlaceCategory.TOURIST_SPOT),
            Map.entry("박물관", PlaceCategory.CULTURE),
            Map.entry("미술관", PlaceCategory.CULTURE),
            Map.entry("문예회관", PlaceCategory.CULTURE)
    );

    /** 카테고리2 는 카테고리3 이 비었을 때만 쓰는 예비 축이다 */
    private static final Map<String, PlaceCategory> BY_CATEGORY2 = Map.of(
            "반려의료", PlaceCategory.VET,
            "반려동물식당카페", PlaceCategory.CAFE,
            "반려동반여행", PlaceCategory.TOURIST_SPOT,
            "반려문화시설", PlaceCategory.CULTURE,
            "반려동물 서비스", PlaceCategory.PET_SUPPLY
    );

    private CategoryMapper() {
    }

    public static PlaceCategory fromCultureData(String category2, String category3) {
        PlaceCategory matched = BY_CATEGORY3.get(trim(category3));
        if (matched != null) {
            return matched;
        }
        return BY_CATEGORY2.getOrDefault(trim(category2), PlaceCategory.ETC);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}