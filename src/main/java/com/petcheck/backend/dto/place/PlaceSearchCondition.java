package com.petcheck.backend.dto.place;

import com.petcheck.backend.entity.pet.PetSize;
import com.petcheck.backend.entity.place.PlaceCategory;

import java.util.Set;

/**
 * 장소 검색 조건. 모든 필드가 null 을 허용하고, null 인 조건은 무시된다.
 * QueryDSL 의 where() 가 null 인자를 건너뛰는 성질을 그대로 쓴다.
 *
 * @param keyword           매장명·주소 부분 일치
 * @param categories        카테고리 다중 선택
 * @param careFacilityOnly  true 면 위탁·돌봄 시설만 (펫시터·애견호텔·유치원)
 * @param petSize           이 크기가 입장 가능한 장소만
 * @param petWeightKg       매장이 kg 상한을 건 경우 이 값과 비교한다
 * @param indoorRequired    true 면 실내 동반 가능한 곳만
 * @param noCarrierOnly     true 면 케이지·캐리어 없이 동반 가능한 곳만
 * @param legalCertifiedOnly true 면 법정 기준 인증 매장만
 * @param minTrustScore     이 점수 이상만. TrustGrade.threshold() 를 넣으면 등급 필터가 된다
 * @param latitude          반경 검색 중심 위도
 * @param longitude         반경 검색 중심 경도
 * @param radiusKm          반경(km)
 * @param sort              정렬 기준. null 이면 TRUST_SCORE
 */
public record PlaceSearchCondition(
        String keyword,
        Set<PlaceCategory> categories,
        Boolean careFacilityOnly,
        PetSize petSize,
        Double petWeightKg,
        Boolean indoorRequired,
        Boolean noCarrierOnly,
        Boolean legalCertifiedOnly,
        Double minTrustScore,
        Double latitude,
        Double longitude,
        Double radiusKm,
        PlaceSortType sort
) {

    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    public boolean hasRadius() {
        return hasCoordinates() && radiusKm != null && radiusKm > 0;
    }

    public PlaceSortType sortOrDefault() {
        if (sort == null) {
            return PlaceSortType.TRUST_SCORE;
        }
        if (sort == PlaceSortType.DISTANCE && !hasCoordinates()) {
            return PlaceSortType.TRUST_SCORE;
        }
        return sort;
    }
}
