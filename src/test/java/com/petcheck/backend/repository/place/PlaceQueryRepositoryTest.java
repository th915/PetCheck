package com.petcheck.backend.repository.place;

import com.petcheck.backend.config.QuerydslConfig;
import com.petcheck.backend.dto.place.PlaceSearchCondition;
import com.petcheck.backend.dto.place.PlaceSortType;
import com.petcheck.backend.dto.place.PlaceSummary;
import com.petcheck.backend.entity.member.Member;
import com.petcheck.backend.entity.member.MemberRole;
import com.petcheck.backend.entity.pet.PetSize;
import com.petcheck.backend.entity.place.PetPolicy;
import com.petcheck.backend.entity.place.Place;
import com.petcheck.backend.entity.place.PlaceCategory;
import com.petcheck.backend.entity.place.SourceType;
import com.petcheck.backend.entity.store.LegalChecklist;
import com.petcheck.backend.entity.store.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QueryDSL 로 짠 동적 검색이 실제 SQL 로 번역되는지 확인한다.
 * <p>
 * 로컬 MySQL 이 떠 있어야 돌기 때문에 "integration" 태그를 달아
 * 기본 ./gradlew test 에서는 제외된다. 실행은:
 * <pre>
 *   ./gradlew integrationTest
 * </pre>
 * H2 를 쓰지 않는 이유: JSON 컬럼과 MySQL 방언이 실제 환경과 달라서
 * 여기서 통과해도 운영에서 깨질 수 있다.
 */
@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QuerydslConfig.class)
class PlaceQueryRepositoryTest {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private TestEntityManager em;

    private Place certifiedCafe;
    private Place plainRestaurant;

    @BeforeEach
    void setUp() {
        Member owner = em.persist(Member.builder()
                .email("owner@petcheck.com").password("hashed")
                .nickname("사장님").role(MemberRole.STORE).build());

        // 소형견만 받는 인증 카페
        certifiedCafe = em.persist(Place.builder()
                .sourceType(SourceType.TOUR_API).externalId("A1")
                .publiclyRegistered(true)
                .name("멍멍 카페").category(PlaceCategory.CAFE)
                .address("광주광역시 동구 충장로 1")
                .latitude(35.1468).longitude(126.9227)
                .petPolicy(PetPolicy.builder()
                        .allowSmall(true).allowMedium(false).allowLarge(false)
                        .indoorAllowed(true).build())
                .build());

        Store store = em.persist(Store.builder()
                .place(certifiedCafe).owner(owner).businessName("멍멍 카페")
                .legalChecklist(LegalChecklist.builder()
                        .partitioned(true).cageProvided(true).leashAnchor(true).seatCover(true)
                        .petTableware(true).petWasteBin(true).entranceSign(true)
                        .vaccinationCheck(true).build())
                .build());
        store.approve();

        // 대형견까지 받지만 매장 등록이 없는 식당 (30km 쯤 떨어진 곳)
        plainRestaurant = em.persist(Place.builder()
                .sourceType(SourceType.CULTURE_DATA).externalId("B1")
                .publiclyRegistered(false)
                .name("바우 식당").category(PlaceCategory.RESTAURANT)
                .address("전라남도 나주시 1")
                .latitude(35.0160).longitude(126.7108)
                .petPolicy(PetPolicy.builder()
                        .allowSmall(true).allowMedium(true).allowLarge(true)
                        .indoorAllowed(false).build())
                .build());

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("크기 필터 — 대형견은 소형견만 받는 카페에서 걸러진다")
    void 크기필터() {
        PlaceSearchCondition condition = new PlaceSearchCondition(
                null, null, null, PetSize.LARGE, null, null, null, null, null, null, null, null);

        Page<PlaceSummary> result = placeRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(PlaceSummary::name)
                .containsExactly("바우 식당");
    }

    @Test
    @DisplayName("법정기준 인증 필터 — 매장 등록이 없는 장소는 제외된다")
    void 인증필터() {
        PlaceSearchCondition condition = new PlaceSearchCondition(
                null, null, null, null, null, null, true, null, null, null, null, null);

        Page<PlaceSummary> result = placeRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("멍멍 카페");
        assertThat(result.getContent().get(0).legalCertified()).isTrue();
    }

    @Test
    @DisplayName("매장 등록이 없으면 legalCertified 는 null 로 내려온다")
    void 미등록장소는_null() {
        PlaceSearchCondition condition = new PlaceSearchCondition(
                "바우", null, null, null, null, null, null, null, null, null, null, null);

        Page<PlaceSummary> result = placeRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getContent().get(0).legalCertified()).isNull();
        assertThat(result.getContent().get(0).isStoreRegistered()).isFalse();
    }

    @Test
    @DisplayName("반경 필터 — 5km 밖의 장소는 걸러진다")
    void 반경필터() {
        PlaceSearchCondition condition = new PlaceSearchCondition(
                null, null, null, null, null, null, null, null,
                35.1468, 126.9227, 5.0, PlaceSortType.DISTANCE);

        Page<PlaceSummary> result = placeRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(PlaceSummary::name)
                .containsExactly("멍멍 카페");
    }

    @Test
    @DisplayName("거리순 정렬 — 중심에 가까운 장소가 먼저 나온다")
    void 거리순정렬() {
        PlaceSearchCondition condition = new PlaceSearchCondition(
                null, null, null, null, null, null, null, null,
                35.1468, 126.9227, null, PlaceSortType.DISTANCE);

        List<PlaceSummary> result = placeRepository.searchForMap(condition, 50);

        assertThat(result)
                .extracting(PlaceSummary::name)
                .containsExactly("멍멍 카페", "바우 식당");
    }

    @Test
    @DisplayName("카테고리 다중 선택은 OR 로 묶인다")
    void 카테고리_다중선택() {
        PlaceSearchCondition condition = new PlaceSearchCondition(
                null, Set.of(PlaceCategory.CAFE, PlaceCategory.RESTAURANT),
                null, null, null, null, null, null, null, null, null, null);

        Page<PlaceSummary> result = placeRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("조건이 전부 null 이면 필터가 하나도 걸리지 않는다")
    void 빈조건() {
        PlaceSearchCondition condition = new PlaceSearchCondition(
                null, null, null, null, null, null, null, null, null, null, null, null);

        Page<PlaceSummary> result = placeRepository.search(condition, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(2);
    }
}
