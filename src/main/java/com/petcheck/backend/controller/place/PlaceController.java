package com.petcheck.backend.controller.place;

import com.petcheck.backend.dto.common.PageResponse;
import com.petcheck.backend.dto.place.MaterializePlaceRequest;
import com.petcheck.backend.dto.place.PlaceDetail;
import com.petcheck.backend.dto.place.PlaceSearchCondition;
import com.petcheck.backend.dto.place.PlaceSummary;
import com.petcheck.backend.external.kakao.KakaoLocalSearchClient;
import com.petcheck.backend.external.kakao.dto.KakaoPlaceResult;
import com.petcheck.backend.service.place.PlaceMaterializationService;
import com.petcheck.backend.service.place.PlaceSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 장소 검색 · 상세. 프론트가 제일 먼저 붙는 API 다.
 * <p>
 * 검색 조건이 12개라 @RequestParam 으로 늘어놓지 않고 @ModelAttribute 로 한 번에 받는다.
 * 스프링이 쿼리 파라미터 이름을 record 의 컴포넌트 이름과 맞춰 생성자로 바인딩한다.
 */
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceSearchService placeSearchService;
    private final KakaoLocalSearchClient kakaoLocalSearchClient;
    private final PlaceMaterializationService placeMaterializationService;

    /**
     * 목록 검색.
     * 예) /api/places?keyword=카페&petSize=SMALL&legalCertifiedOnly=true&sort=TRUST_SCORE&page=0&size=20
     */
    @GetMapping
    public PageResponse<PlaceSummary> search(@ModelAttribute PlaceSearchCondition condition,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(placeSearchService.search(condition, pageable));
    }

    /**
     * 지도용. 페이지 번호가 의미 없으므로 상한만 두고 배열로 준다.
     * 예) /api/places/map?latitude=35.1468&longitude=126.9227&radiusKm=3&sort=DISTANCE
     */
    @GetMapping("/map")
    public List<PlaceSummary> searchForMap(@ModelAttribute PlaceSearchCondition condition,
                                           @RequestParam(defaultValue = "200") int limit) {
        return placeSearchService.searchForMap(condition, limit);
    }

    @GetMapping("/{id}")
    public PlaceDetail detail(@PathVariable Long id) {
        return placeSearchService.getDetail(id);
    }

    /**
     * DB(공공데이터 4종)에 없는 장소를 카카오맵에서 찾는다. 저장하지 않고 그대로 보여만 준다.
     * 예) /api/places/external-search?keyword=대형견카페&latitude=35.1&longitude=126.9
     */
    @GetMapping("/external-search")
    public List<KakaoPlaceResult> externalSearch(@RequestParam String keyword,
                                                  @RequestParam(required = false) Double latitude,
                                                  @RequestParam(required = false) Double longitude) {
        return kakaoLocalSearchClient.search(keyword, latitude, longitude);
    }

    /**
     * 카카오 검색 결과를 처음 즐겨찾기·일정·리뷰에 쓰는 순간 실제 Place 로 만든다.
     * 이미 만들어진 적 있으면 그 id 를 그대로 돌려준다(중복 생성 안 됨).
     */
    @PostMapping("/from-kakao")
    public Map<String, Long> materializeFromKakao(@Valid @RequestBody MaterializePlaceRequest request) {
        return Map.of("id", placeMaterializationService.materialize(request));
    }
}