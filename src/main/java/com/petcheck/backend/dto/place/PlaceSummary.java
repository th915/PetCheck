package com.petcheck.backend.dto.place;

import com.petcheck.backend.entity.place.PlaceCategory;
import com.petcheck.backend.entity.place.TrustGrade;

import java.time.LocalDateTime;

/**
 * 목록·지도 응답 한 건. 엔티티를 그대로 내보내지 않는 이유는
 * Place 를 반환하면 LAZY 연관이 딸려 나가면서 직렬화 시점에 추가 쿼리가 터지기 때문이다.
 * QueryDSL Projections.constructor 가 이 생성자를 그대로 호출한다.
 *
 * @param legalCertified null = 매장 등록이 없는 장소. false = 등록했으나 미인증
 */
public record PlaceSummary(
        Long id,
        String name,
        PlaceCategory category,
        String address,
        Double latitude,
        Double longitude,
        String imageUrl,
        Double trustScore,
        TrustGrade trustGrade,
        Integer reviewCount,
        LocalDateTime lastVerifiedAt,
        Boolean legalCertified
) {

    public boolean isStoreRegistered() {
        return legalCertified != null;
    }

    /**
     * 중심 좌표로부터의 대략적인 거리(km).
     * 정렬은 DB 가 하고, 화면에 찍을 숫자만 여기서 만든다.
     */
    public double distanceKmFrom(double centerLat, double centerLng) {
        double dLat = (latitude - centerLat) * 111.32;
        double dLng = (longitude - centerLng) * 111.32 * Math.cos(Math.toRadians(centerLat));
        return Math.sqrt(dLat * dLat + dLng * dLng);
    }
}
