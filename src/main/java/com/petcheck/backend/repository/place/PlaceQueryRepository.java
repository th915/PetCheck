package com.petcheck.backend.repository.place;

import com.petcheck.backend.dto.place.PlaceSearchCondition;
import com.petcheck.backend.dto.place.PlaceSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 동적 조건 검색. Spring Data 의 커스텀 리포지토리 규약에 따라
 * PlaceRepository 가 이 인터페이스를 상속하고,
 * 구현체 이름은 반드시 {인터페이스명}Impl 이어야 자동으로 엮인다.
 */
public interface PlaceQueryRepository {

    /** 목록·리스트 화면 — 페이지네이션이 필요한 검색 */
    Page<PlaceSummary> search(PlaceSearchCondition condition, Pageable pageable);

    /**
     * 지도 화면 — 현재 보이는 영역 안의 장소를 한 번에 뿌린다.
     * 페이지 번호가 의미 없으므로 상한만 두고 List 로 돌려준다.
     */
    List<PlaceSummary> searchForMap(PlaceSearchCondition condition, int limit);
}
