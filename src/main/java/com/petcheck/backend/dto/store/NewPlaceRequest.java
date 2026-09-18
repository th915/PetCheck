package com.petcheck.backend.dto.store;

import com.petcheck.backend.entity.place.PlaceCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 공공데이터에 아직 없는 매장을 사장님이 직접 등록할 때 쓴다.
 * 신규 개업이거나 데이터 갱신 주기에 걸려 누락된 경우가 여기 해당한다.
 */
public record NewPlaceRequest(

        @NotBlank(message = "매장 이름은 필수입니다")
        String name,

        @NotNull(message = "업종을 선택해주세요")
        PlaceCategory category,

        @NotBlank(message = "주소는 필수입니다")
        String address,

        String phone,

        @NotNull(message = "위도는 필수입니다")
        @DecimalMin(value = "33.0", message = "국내 좌표가 아닙니다")
        @DecimalMax(value = "39.0", message = "국내 좌표가 아닙니다")
        Double latitude,

        @NotNull(message = "경도는 필수입니다")
        @DecimalMin(value = "124.0", message = "국내 좌표가 아닙니다")
        @DecimalMax(value = "132.0", message = "국내 좌표가 아닙니다")
        Double longitude
) {
}