package com.petcheck.backend.external.kakao.dto;

import com.petcheck.backend.entity.place.PlaceCategory;

/**
 * 카카오 로컬 키워드 검색 결과 한 건. DB 에 저장되지 않은, 아직 "실체화"되지 않은 장소다.
 * 프론트가 즐겨찾기·일정·리뷰 중 하나를 시도하면 kakaoId 로
 * {@code POST /api/places/from-kakao} 를 불러 실제 Place 로 바뀐다.
 */
public record KakaoPlaceResult(
        String kakaoId,
        String name,
        String address,
        String phone,
        Double latitude,
        Double longitude,
        PlaceCategory category
) {
}
