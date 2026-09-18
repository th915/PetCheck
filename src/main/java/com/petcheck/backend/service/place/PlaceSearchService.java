package com.petcheck.backend.service.place;

import com.petcheck.backend.common.exception.BusinessException;
import com.petcheck.backend.common.exception.ErrorCode;
import com.petcheck.backend.dto.place.PlaceDetail;
import com.petcheck.backend.dto.place.PlaceSearchCondition;
import com.petcheck.backend.dto.place.PlaceSummary;
import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.entity.place.SourceType;
import com.petcheck.backend.external.tour.TourApiClient;
import com.petcheck.backend.external.tour.dto.TourPetDetail;
import com.petcheck.backend.repository.place.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceSearchService {

    /** 지도 한 화면에 뿌리는 최대 개수. 이보다 많으면 브라우저가 먼저 버벅인다. */
    private static final int MAX_MAP_RESULTS = 300;

    private final PlaceRepository placeRepository;
    private final TourApiClient tourApiClient;

    public Page<PlaceSummary> search(PlaceSearchCondition condition, Pageable pageable) {
        return placeRepository.search(condition, pageable);
    }

    public List<PlaceSummary> searchForMap(PlaceSearchCondition condition, int limit) {
        return placeRepository.searchForMap(condition, Math.min(limit, MAX_MAP_RESULTS));
    }

    /**
     * 상세 조회. 저장된 정보를 먼저 읽고, TourAPI 로 보강한다.
     * <p>
     * 외부 호출이 실패해도 상세 화면은 떠야 하므로 TourApiClient 는 null 을 돌려준다.
     * 여기서 예외로 바꾸지 않는다.
     */
    public PlaceDetail getDetail(Long id) {
        Place place = placeRepository.findDetailById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PLACE_NOT_FOUND,
                        "장소를 찾을 수 없습니다: id=" + id));

        return PlaceDetail.from(place, fetchTourInfo(place));
    }

    /** TourAPI 로 들어온 장소만 보강 대상이다. 다른 출처는 contentId 가 없다. */
    private TourPetDetail fetchTourInfo(Place place) {
        if (place.getSourceType() != SourceType.TOUR_API || place.getExternalId() == null) {
            return null;
        }
        return tourApiClient.fetchPetDetail(place.getExternalId());
    }
}