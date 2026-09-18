package com.petcheck.backend.repository.place;

import com.petcheck.backend.dto.place.PlaceSearchCondition;
import com.petcheck.backend.dto.place.PlaceSortType;
import com.petcheck.backend.dto.place.PlaceSummary;
import com.petcheck.backend.entity.pet.PetSize;
import com.petcheck.backend.entity.place.PlaceCategory;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;

import static com.petcheck.backend.entity.place.QPlace.place;
import static com.petcheck.backend.entity.store.QStore.store;

/**
 * 동반 조건 다중 필터 + 신뢰도순 정렬 + 지도 반경 검색.
 * <p>
 * 각 조건은 BooleanExpression 을 돌려주는 메서드로 쪼개져 있고,
 * 조건이 없으면 null 을 돌려준다. QueryDSL 의 where() 는 null 인자를 그냥 건너뛰므로
 * if 문으로 쿼리를 조립할 필요가 없다. 조건을 하나 늘리는 비용이 메서드 하나다.
 */
@Repository
@RequiredArgsConstructor
public class PlaceQueryRepositoryImpl implements PlaceQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<PlaceSummary> search(PlaceSearchCondition condition, Pageable pageable) {
        List<PlaceSummary> content = baseQuery(condition)
                .orderBy(orderBy(condition))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(place.count())
                .from(place)
                .leftJoin(place.store, store)
                .where(predicates(condition));

        // 마지막 페이지이거나 첫 페이지가 다 차지 않으면 count 쿼리를 아예 보내지 않는다.
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<PlaceSummary> searchForMap(PlaceSearchCondition condition, int limit) {
        return baseQuery(condition)
                .orderBy(orderBy(condition))
                .limit(limit)
                .fetch();
    }

    private JPAQuery<PlaceSummary> baseQuery(PlaceSearchCondition condition) {
        return queryFactory
                .select(Projections.constructor(PlaceSummary.class,
                        place.id,
                        place.name,
                        place.category,
                        place.address,
                        place.latitude,
                        place.longitude,
                        place.imageUrl,
                        place.trustScore,
                        place.trustGrade,
                        place.reviewCount,
                        place.lastVerifiedAt,
                        store.legalCertified))
                .from(place)
                .leftJoin(place.store, store)
                .where(predicates(condition));
    }

    private BooleanExpression[] predicates(PlaceSearchCondition c) {
        return new BooleanExpression[]{
                petAllowed(),
                keywordContains(c.keyword()),
                categoryIn(c.categories()),
                careFacilityOnly(c.careFacilityOnly()),
                admitsPet(c.petSize(), c.petWeightKg()),
                indoorRequired(c.indoorRequired()),
                noCarrierOnly(c.noCarrierOnly()),
                legalCertifiedOnly(c.legalCertifiedOnly()),
                minTrustScore(c.minTrustScore()),
                withinBoundingBox(c)
        };
    }

    // --- 조건 ---

    /**
     * 동반 불가로 확인된 장소를 검색에서 뺀다.
     * <p>
     * 조건이 아니라 항상 걸리는 필터다. 반려동물과 갈 수 있는 곳을 찾는 서비스이므로
     * 불가한 곳이 목록에 섞이면 그 자체가 헛걸음을 만든다.
     * <p>
     * null(정보 없음)은 남긴다. "확인이 안 된 곳"과 "안 되는 곳"은 다르고,
     * 전자는 리뷰로 검증해 나갈 대상이다.
     */
    private BooleanExpression petAllowed() {
        return place.petPolicy.petAllowed.isNull()
                .or(place.petPolicy.petAllowed.isTrue());
    }

    private BooleanExpression keywordContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        return place.name.containsIgnoreCase(keyword)
                .or(place.address.containsIgnoreCase(keyword));
    }

    private BooleanExpression categoryIn(Collection<PlaceCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }
        return place.category.in(categories);
    }

    private BooleanExpression careFacilityOnly(Boolean careFacilityOnly) {
        if (careFacilityOnly == null) {
            return null;
        }
        return careFacilityOnly
                ? place.category.in(PlaceCategory.careFacilities())
                : place.category.notIn(PlaceCategory.careFacilities());
    }

    /**
     * PetPolicy.admits() 와 같은 규칙을 SQL 로 옮긴 것.
     * <p>
     * 매장이 kg 상한을 직접 걸었으면 그 값이 우선하고, 없으면 크기 허용 여부를 본다.
     * 크기 허용 여부를 JSON 이 아니라 boolean 컬럼 세 개로 둔 이유가 여기 있다.
     * JSON 이었다면 이 한 줄이 JSON_CONTAINS 호출이 되고 인덱스를 못 탔다.
     */
    private BooleanExpression admitsPet(PetSize size, Double weightKg) {
        if (size == null) {
            return null;
        }
        BooleanExpression sizeAllowed = switch (size) {
            case SMALL -> place.petPolicy.allowSmall.isTrue();
            case MEDIUM -> place.petPolicy.allowMedium.isTrue();
            case LARGE -> place.petPolicy.allowLarge.isTrue();
        };

        if (weightKg == null) {
            return sizeAllowed;
        }
        return place.petPolicy.weightLimitKg.isNull().and(sizeAllowed)
                .or(place.petPolicy.weightLimitKg.goe(weightKg));
    }

    private BooleanExpression indoorRequired(Boolean indoorRequired) {
        if (indoorRequired == null || !indoorRequired) {
            return null;
        }
        return place.petPolicy.indoorAllowed.isTrue();
    }

    private BooleanExpression noCarrierOnly(Boolean noCarrierOnly) {
        if (noCarrierOnly == null || !noCarrierOnly) {
            return null;
        }
        return place.petPolicy.carrierRequired.isFalse();
    }

    /**
     * left join 이라 매장 등록이 없는 장소는 store.legalCertified 가 null 이다.
     * isTrue() 는 null 을 걸러내므로 IS NULL 을 따로 쓸 필요가 없다.
     */
    private BooleanExpression legalCertifiedOnly(Boolean legalCertifiedOnly) {
        if (legalCertifiedOnly == null || !legalCertifiedOnly) {
            return null;
        }
        return store.legalCertified.isTrue();
    }

    private BooleanExpression minTrustScore(Double minTrustScore) {
        if (minTrustScore == null) {
            return null;
        }
        return place.trustScore.goe(minTrustScore);
    }

    /**
     * 반경 검색을 정사각형 경계로 근사한다.
     * <p>
     * 원형 반경을 정확히 구하려면 ST_Distance_Sphere 같은 DB 함수가 필요한데,
     * 함수 결과에는 인덱스가 걸리지 않아 장소 전체를 훑게 된다.
     * (latitude, longitude) 인덱스를 태우려면 범위 비교여야 한다.
     * <p>
     * 경계 밖으로 최대 41% 더 넓게 잡히지만, 정렬이 거리순이고
     * 화면에는 상위 N개만 나가므로 사용자가 체감하지 못한다.
     */
    private BooleanExpression withinBoundingBox(PlaceSearchCondition c) {
        if (!c.hasRadius()) {
            return null;
        }
        double latDelta = c.radiusKm() / 111.32;
        double lngDelta = c.radiusKm() / (111.32 * Math.cos(Math.toRadians(c.latitude())));

        return place.latitude.between(c.latitude() - latDelta, c.latitude() + latDelta)
                .and(place.longitude.between(c.longitude() - lngDelta, c.longitude() + lngDelta));
    }

    // --- 정렬 ---

    /**
     * 정렬 기준 끝에 항상 id 를 붙인다.
     * <p>
     * 리뷰가 쌓이기 전에는 모든 장소의 trustScore 가 0.5, reviewCount 가 0 이다.
     * 전부 동점이면 DB 가 돌려주는 순서에 보장이 없어, 페이지를 넘길 때
     * 같은 장소가 두 번 나오거나 아예 빠진다. id 는 유일하므로 순서가 확정된다.
     */
    private OrderSpecifier<?>[] orderBy(PlaceSearchCondition c) {
        PlaceSortType sort = c.sortOrDefault();

        return switch (sort) {
            case DISTANCE -> new OrderSpecifier<?>[]{
                    squaredDistance(c).asc(), place.trustScore.desc(), place.id.asc()};
            case REVIEW_COUNT -> new OrderSpecifier<?>[]{
                    place.reviewCount.desc(), place.trustScore.desc(), place.id.asc()};
            case RECENTLY_VERIFIED -> new OrderSpecifier<?>[]{
                    place.lastVerifiedAt.desc().nullsLast(), place.trustScore.desc(), place.id.asc()};
            case TRUST_SCORE -> new OrderSpecifier<?>[]{
                    recommendScore().desc(), place.id.asc()};
        };
    }

    /**
     * "추천순" 은 신뢰도 하나만 보지 않는다.
     * <p>
     * 인증 매장 가점(+0.3) + 신뢰도 점수(0~1) + 리뷰 표본 가점(최대 +0.2, 20건부터 상한)을
     * 더한 값으로 정렬한다. 표본이 없는 신규 장소(trustScore=0.5)가 인증도 없이
     * 상단에 깔리는 것을 막고, 인증·검증 표본이 많은 장소가 앞으로 오게 한다.
     */
    private NumberExpression<Double> recommendScore() {
        NumberExpression<Double> certifiedBonus = new CaseBuilder()
                .when(store.legalCertified.isTrue()).then(0.3)
                .otherwise(0.0);

        NumberExpression<Integer> cappedReviewCount = new CaseBuilder()
                .when(place.reviewCount.gt(20)).then(20)
                .otherwise(place.reviewCount);
        NumberExpression<Double> reviewBonus = cappedReviewCount.doubleValue().multiply(0.01);

        return place.trustScore.add(certifiedBonus).add(reviewBonus);
    }

    /**
     * 거리 정렬용 제곱거리. 정렬만 하면 되므로 제곱근을 씌우지 않는다.
     * <p>
     * 위도 1도는 어디서나 약 111km 지만 경도 1도는 위도에 따라 줄어든다.
     * cos(중심위도)를 자바에서 상수로 계산해 곱하는 것으로 그 보정을 끝낸다.
     * DB 함수를 하나도 쓰지 않으므로 MySQL 방언에 묶이지 않는다.
     */
    private NumberExpression<Double> squaredDistance(PlaceSearchCondition c) {
        double cosLat = Math.cos(Math.toRadians(c.latitude()));

        NumberExpression<Double> dLat = place.latitude.subtract(c.latitude());
        NumberExpression<Double> dLng = place.longitude.subtract(c.longitude()).multiply(cosLat);

        return dLat.multiply(dLat).add(dLng.multiply(dLng));
    }
}
