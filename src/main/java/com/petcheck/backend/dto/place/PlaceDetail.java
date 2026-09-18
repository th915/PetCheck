package com.petcheck.backend.dto.place;

import com.petcheck.backend.entity.place.PetPolicy;
import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.entity.place.PlaceCategory;
import com.petcheck.backend.entity.place.TrustGrade;
import com.petcheck.backend.entity.store.LegalChecklist;
import com.petcheck.backend.entity.store.Store;
import com.petcheck.backend.external.tour.dto.TourPetDetail;

import java.time.LocalDateTime;

/**
 * 장소 상세 화면 한 건.
 * <p>
 * PetPolicy 와 LegalChecklist 는 엔티티 타입이지만 그대로 내보낸다.
 * 둘 다 연관관계가 없는 값 객체라 직렬화 중에 추가 쿼리가 나갈 일이 없고,
 * 별도 View 를 하나 더 만들면 필드만 그대로 옮기는 코드가 는다.
 *
 * @param store    매장 등록이 없으면 null
 * @param tourInfo TourAPI 보강 정보. 호출 실패하거나 대상이 아니면 null
 */
public record PlaceDetail(
        Long id,
        String name,
        PlaceCategory category,
        String address,
        String phone,
        Double latitude,
        Double longitude,
        String imageUrl,
        PetPolicy petPolicy,
        Double trustScore,
        TrustGrade trustGrade,
        int reviewCount,
        int matchedReviewCount,
        LocalDateTime lastVerifiedAt,
        StoreInfo store,
        TourPetDetail tourInfo
) {

    public record StoreInfo(
            String businessName,
            boolean legalCertified,
            LegalChecklist legalChecklist,
            String proofImageUrl,
            LocalDateTime approvedAt
    ) {

        static StoreInfo from(Store store) {
            if (store == null) {
                return null;
            }
            return new StoreInfo(
                    store.getBusinessName(),
                    store.isLegalCertified(),
                    store.getLegalChecklist(),
                    store.getProofImageUrl(),
                    store.getApprovedAt());
        }
    }

    public static PlaceDetail from(Place place, TourPetDetail tourInfo) {
        return new PlaceDetail(
                place.getId(),
                place.getName(),
                place.getCategory(),
                place.getAddress(),
                place.getPhone(),
                place.getLatitude(),
                place.getLongitude(),
                place.getImageUrl(),
                place.getPetPolicy(),
                place.getTrustScore(),
                place.getTrustGrade(),
                place.getReviewCount(),
                place.getMatchedReviewCount(),
                place.getLastVerifiedAt(),
                StoreInfo.from(place.getStore()),
                tourInfo
        );
    }
}