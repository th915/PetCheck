package com.petcheck.backend.external.dto;

import com.petcheck.backend.entity.place.PetPolicy;
import com.petcheck.backend.entity.place.PlaceCategory;
import com.petcheck.backend.entity.place.SourceType;

/**
 * 출처가 다른 데이터를 Place 로 넣기 직전의 공통 중간 형태.
 * <p>
 * 제안서 "학습·고민 포인트" 3번(공공데이터 4종의 스키마 불일치)에 대한 답이 이 record 다.
 * 기관이 늘어나도 Place 와 PlaceSyncService 는 그대로 두고
 * "그 기관 응답 → PlaceSyncData" 변환기 하나만 새로 만들면 된다.
 */
public record PlaceSyncData(
        SourceType sourceType,
        String externalId,
        String name,
        PlaceCategory category,
        String address,
        String phone,
        Double latitude,
        Double longitude,
        String imageUrl,
        PetPolicy petPolicy
) {

    /** 좌표가 없으면 지도에 못 올리므로 아예 버린다. */
    public boolean isUsable() {
        return name != null && !name.isBlank()
                && latitude != null && longitude != null
                && address != null && !address.isBlank();
    }
}