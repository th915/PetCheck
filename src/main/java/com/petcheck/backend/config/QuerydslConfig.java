package com.petcheck.backend.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL 은 스타터가 없어서 JPAQueryFactory 를 직접 빈으로 등록해야 한다.
 * PlaceQueryRepositoryImpl 과 TrustStatsRepositoryImpl 이 이 빈을 주입받는다.
 */
@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
