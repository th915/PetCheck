package com.petcheck.backend.repository.trust;

import com.petcheck.backend.dto.trust.SourceAccuracy;
import com.petcheck.backend.dto.trust.TrustTrendPoint;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static com.petcheck.backend.entity.place.QPlace.place;
import static com.petcheck.backend.entity.store.QStore.store;
import static com.petcheck.backend.entity.trust.QTrustScoreHistory.trustScoreHistory;

@Repository
@RequiredArgsConstructor
public class TrustStatsRepositoryImpl implements TrustStatsRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Double averageTrustScore() {
        return queryFactory
                .select(place.trustScore.avg())
                .from(place)
                .fetchOne();
    }

    /**
     * 매장이 등록한 장소와 공공데이터만 있는 장소를 같은 잣대로 비교한다.
     * <p>
     * Place.store 는 @OneToOne(mappedBy) 이라 place 테이블에 FK 가 없다.
     * 따라서 place.store.isNull() 같은 표현은 SQL 로 번역되지 않는다.
     * left join 을 걸고 store.id 의 null 여부를 보는 것이 유일한 방법이다.
     */
    @Override
    public List<SourceAccuracy> compareAccuracyBySource() {
        return List.of(
                accuracyOf(true, store.id.isNotNull()),
                accuracyOf(false, store.id.isNull())
        );
    }

    private SourceAccuracy accuracyOf(boolean storeRegistered, BooleanExpression registration) {
        // 리뷰가 0건인 장소는 분모가 0이라 아예 제외한다.
        NumberExpression<Double> matchRate = place.matchedReviewCount.doubleValue()
                .divide(place.reviewCount.doubleValue());

        Tuple result = queryFactory
                .select(place.count(), matchRate.avg())
                .from(place)
                .leftJoin(place.store, store)
                .where(place.reviewCount.gt(0), registration)
                .fetchOne();

        if (result == null) {
            return new SourceAccuracy(storeRegistered, 0L, null);
        }

        // 표현식이 아니라 인덱스로 꺼낸다. select 에 쓴 것과 같은 객체를 다시 만들 필요가 없다.
        Long count = result.get(0, Long.class);
        Double average = result.get(1, Double.class);

        return new SourceAccuracy(storeRegistered, count == null ? 0L : count, average);
    }

    /**
     * 일자별 평균 신뢰도.
     * <p>
     * format(x as 'yyyy-MM-dd') 는 Hibernate 6 가 자체적으로 제공하는 HQL 함수라
     * DATE_FORMAT 같은 MySQL 전용 함수를 부르지 않는다.
     * 스냅샷이 하루에 여러 번 쌓여도 날짜 단위로 뭉쳐진다.
     */
    @Override
    public List<TrustTrendPoint> trendSince(LocalDateTime from) {
        StringTemplate day = Expressions.stringTemplate(
                "format({0} as 'yyyy-MM-dd')", trustScoreHistory.calculatedAt);

        return queryFactory
                .select(Projections.constructor(TrustTrendPoint.class,
                        day,
                        trustScoreHistory.trustScore.avg()))
                .from(trustScoreHistory)
                .where(trustScoreHistory.calculatedAt.goe(from))
                .groupBy(day)
                .orderBy(day.asc())
                .fetch();
    }
}
