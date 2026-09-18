package com.petcheck.backend.dto.place;

import com.petcheck.backend.entity.place.PlaceCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 카카오맵 검색 결과를 실제 Place 로 만들 때 프론트가 보내는 값.
 * 검색 응답을 그대로 되돌려 보내는 것뿐이라 서버가 다시 카카오를 호출하지 않는다.
 *
 * @param kakaoId 카카오 장소 ID. (KAKAO_API, kakaoId) 로 중복 생성을 막는다
 */
public record MaterializePlaceRequest(

        @NotBlank(message = "카카오 장소 ID가 필요합니다")
        String kakaoId,

        @NotBlank(message = "장소명이 필요합니다")
        @Size(max = 200)
        String name,

        @NotBlank(message = "주소가 필요합니다")
        @Size(max = 300)
        String address,

        String phone,

        @NotNull
        @DecimalMin(value = "33.0", message = "위도 범위를 벗어났습니다")
        @DecimalMax(value = "39.0", message = "위도 범위를 벗어났습니다")
        Double latitude,

        @NotNull
        @DecimalMin(value = "124.0", message = "경도 범위를 벗어났습니다")
        @DecimalMax(value = "132.0", message = "경도 범위를 벗어났습니다")
        Double longitude,

        @NotNull(message = "카테고리가 필요합니다")
        PlaceCategory category
) {
}
