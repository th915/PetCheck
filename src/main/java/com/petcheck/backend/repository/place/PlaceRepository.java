package com.petcheck.backend.repository.place;

import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.entity.place.SourceType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;

/**
 * 기본 CRUD 와 고정 쿼리. 조건이 동적으로 붙는 검색은 PlaceQueryRepository 가 맡는다.
 */
public interface PlaceRepository extends JpaRepository<Place, Long>, PlaceQueryRepository {

    /** 공공데이터 재수집 시 이미 들어온 장소인지 확인한다. */
    Optional<Place> findBySourceTypeAndExternalId(SourceType sourceType, String externalId);

    /**
     * 신뢰도 배치용. store 를 함께 끌어오는 것이 핵심이다.
     * <p>
     * Place.priorScore() 가 store 를 읽는데, @OneToOne(mappedBy) 는
     * null 일 수 있어 Hibernate 가 프록시를 만들지 못하고 장소마다 추가 쿼리를 낸다.
     * join fetch 가 없으면 장소 1만 건에 쿼리 1만 번이 나간다.
     * <p>
     * offset 페이징 대신 id 커서를 쓴다. 배치가 뒤로 갈수록 offset 이 느려지기 때문이다.
     */
    @Query("""
            select p
            from Place p
            left join fetch p.store
            where p.id > :lastId
              and p.reviewCount > 0
            order by p.id asc
            """)
    List<Place> findBatchForRecalculation(@Param("lastId") Long lastId, Pageable pageable);

    /**
     * 매장 상세 화면. store 와 owner 까지 한 번에 가져온다.
     */
    @Query("""
            select p
            from Place p
            left join fetch p.store s
            left join fetch s.owner
            where p.id = :id
            """)
    Optional<Place> findDetailById(@Param("id") Long id);

    /**
     * 리뷰 기여분을 원자적으로 더한다(삭제 시엔 음수로 뺀다).
     * <p>
     * 엔티티를 읽어 필드를 고치는 방식이면 같은 장소에 동시에 리뷰가 들어올 때
     * 각자 읽은 값 위에 덮어써서 일부가 사라진다. DB 가 더하게 맡겨야 안전하다.
     * <p>
     * clearAutomatically 로 영속성 컨텍스트를 비워야 이어지는 조회가 갱신된 값을 읽는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Place p
            set p.decaySum = p.decaySum + :decayDelta,
                p.decayMatchSum = p.decayMatchSum + :matchDelta,
                p.reviewCount = p.reviewCount + :countDelta,
                p.matchedReviewCount = p.matchedReviewCount + :matchCountDelta
            where p.id = :placeId
            """)
    void addReviewContribution(@Param("placeId") Long placeId,
                               @Param("decayDelta") double decayDelta,
                               @Param("matchDelta") double matchDelta,
                               @Param("countDelta") int countDelta,
                               @Param("matchCountDelta") int matchCountDelta);

    /**
     * 정규화된 이름이 같으면서 반경 안에 있는 장소가 몇 개인지.
     * MySQL 함수를 써야 해서 네이티브 쿼리로 둔다.
     * 좌표 범위로 먼저 줄인 뒤 이름을 비교하므로 전체 스캔이 되지 않는다.
     */
    @Query(value = """
            select count(*) from place
            where latitude between :minLat and :maxLat
              and longitude between :minLng and :maxLng
              and lower(regexp_replace(name, '[^가-힣a-zA-Z0-9]', '')) = :nameKey
            """, nativeQuery = true)
    long countSimilarNamedNearby(@Param("nameKey") String nameKey,
                                 @Param("minLat") double minLat, @Param("maxLat") double maxLat,
                                 @Param("minLng") double minLng, @Param("maxLng") double maxLng);
}
