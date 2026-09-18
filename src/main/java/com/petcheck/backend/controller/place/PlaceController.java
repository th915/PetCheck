package com.petcheck.backend.controller.place;

import com.petcheck.backend.dto.common.PageResponse;
import com.petcheck.backend.dto.place.PlaceDetail;
import com.petcheck.backend.dto.place.PlaceSearchCondition;
import com.petcheck.backend.dto.place.PlaceSummary;
import com.petcheck.backend.service.place.PlaceSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}