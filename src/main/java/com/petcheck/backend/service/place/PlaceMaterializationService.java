package com.petcheck.backend.service.place;

import com.petcheck.backend.dto.place.MaterializePlaceRequest;
import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.entity.place.SourceType;
import com.petcheck.backend.repository.place.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카카오맵 검색 결과(아직 우리 DB에 없는 장소)를 즐겨찾기·일정·리뷰가 가능한
 * 실제 Place 로 바꾼다.
 * <p>
 * 매장 등록 없이도 이용자가 장소를 쓸 수 있게 하는 것이 목적이라
 * 동반 조건(PetPolicy)은 전혀 모르는 채로 시작한다 — petAllowed 가 null 이면
 * PlaceQueryRepositoryImpl.petAllowed() 필터가 "확인 안 된 곳"으로 남겨두므로
 * 검색에서 빠지지 않는다. 이후 리뷰가 쌓이며 신뢰도가 채워진다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PlaceMaterializationService {

    private final PlaceRepository placeRepository;

    /**
     * 이미 실체화된 적 있으면 그 id 를 그대로 돌려준다.
     * 같은 카카오 장소를 여러 사용자가 잇따라 즐겨찾기해도 행이 하나만 생기게 하는 지점이다.
     */
    public Long materialize(MaterializePlaceRequest request) {
        return placeRepository.findBySourceTypeAndExternalId(SourceType.KAKAO_API, request.kakaoId())
                .map(Place::getId)
                .orElseGet(() -> createFrom(request));
    }

    /**
     * 동시에 같은 카카오 장소를 처음 클릭한 두 요청이 둘 다 findBy 를 통과할 수 있다.
     * 그러면 uk_place_source 유니크 제약이 뒤늦은 쪽을 막는데, 그때는 실패가 아니라
     * 먼저 커밋된 행을 찾아 돌려주는 게 맞다 — 사용자 입장에서는 "즐겨찾기가 됐다"일 뿐이다.
     */
    private Long createFrom(MaterializePlaceRequest request) {
        Place place = Place.builder()
                .sourceType(SourceType.KAKAO_API)
                .externalId(request.kakaoId())
                .publiclyRegistered(false)
                .name(request.name())
                .category(request.category())
                .address(request.address())
                .phone(request.phone())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .petPolicy(null)
                .build();

        try {
            return placeRepository.saveAndFlush(place).getId();
        } catch (DataIntegrityViolationException e) {
            return placeRepository.findBySourceTypeAndExternalId(SourceType.KAKAO_API, request.kakaoId())
                    .map(Place::getId)
                    .orElseThrow(() -> e);
        }
    }
}
