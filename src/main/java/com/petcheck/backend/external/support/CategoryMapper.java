package com.petcheck.backend.external.support;

import com.petcheck.backend.entity.place.PlaceCategory;

import java.util.LinkedHashMap;
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

    /**
     * 카카오 로컬 검색의 category_name(예: "의료,건강 > 동물병원", "음식점 > 카페 > 커피전문점")을 옮긴다.
     * <p>
     * 카카오 카테고리는 문화정보원과 달리 값이 열려 있어(사실상 무한) 정확 매핑표를 만들 수 없다.
     * 부분 문자열 매칭으로 가되, 더 구체적인(반려동물 전용) 키워드를 먼저 본다 —
     * "동물병원"이 "병원"보다 먼저 걸려야 VET 으로 가고 ETC 로 새지 않는다.
     */
    private static final Map<String, PlaceCategory> KAKAO_KEYWORDS = new LinkedHashMap<>();

    static {
        KAKAO_KEYWORDS.put("동물병원", PlaceCategory.VET);
        KAKAO_KEYWORDS.put("동물약국", PlaceCategory.PET_PHARMACY);
        KAKAO_KEYWORDS.put("애견미용", PlaceCategory.PET_GROOMING);
        KAKAO_KEYWORDS.put("펫미용", PlaceCategory.PET_GROOMING);
        KAKAO_KEYWORDS.put("애견호텔", PlaceCategory.PET_HOTEL);
        KAKAO_KEYWORDS.put("애견유치원", PlaceCategory.PET_KINDERGARTEN);
        KAKAO_KEYWORDS.put("펫시터", PlaceCategory.PET_SITTER);
        KAKAO_KEYWORDS.put("반려동물용품", PlaceCategory.PET_SUPPLY);
        KAKAO_KEYWORDS.put("애견용품", PlaceCategory.PET_SUPPLY);
        KAKAO_KEYWORDS.put("카페", PlaceCategory.CAFE);
        KAKAO_KEYWORDS.put("음식점", PlaceCategory.RESTAURANT);
        KAKAO_KEYWORDS.put("숙박", PlaceCategory.ACCOMMODATION);
        KAKAO_KEYWORDS.put("펜션", PlaceCategory.ACCOMMODATION);
        KAKAO_KEYWORDS.put("호텔", PlaceCategory.ACCOMMODATION);
        KAKAO_KEYWORDS.put("모텔", PlaceCategory.ACCOMMODATION);
        KAKAO_KEYWORDS.put("관광", PlaceCategory.TOURIST_SPOT);
        KAKAO_KEYWORDS.put("명소", PlaceCategory.TOURIST_SPOT);
        KAKAO_KEYWORDS.put("문화", PlaceCategory.CULTURE);
        KAKAO_KEYWORDS.put("박물관", PlaceCategory.CULTURE);
        KAKAO_KEYWORDS.put("미술관", PlaceCategory.CULTURE);
        KAKAO_KEYWORDS.put("병원", PlaceCategory.VET);
        KAKAO_KEYWORDS.put("약국", PlaceCategory.PET_PHARMACY);
        KAKAO_KEYWORDS.put("미용", PlaceCategory.PET_GROOMING);
    }

    public static PlaceCategory fromKakaoCategoryName(String categoryName) {
        String value = trim(categoryName);
        for (Map.Entry<String, PlaceCategory> entry : KAKAO_KEYWORDS.entrySet()) {
            if (value.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return PlaceCategory.ETC;
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}