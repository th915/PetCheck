package com.petcheck.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 공공데이터 실시간 호출의 단기 캐시.
 * <p>
 * DB 에 데이터를 최소로 두고 API 를 호출하는 구조라, 같은 장소를 여러 사용자가
 * 연속으로 열면 그때마다 외부 호출이 나간다. 공공데이터포털에는 일일 호출 한도가 있고
 * 한도를 넘기면 서비스가 통째로 멈춘다. 10분 캐시가 그 사이를 막는다.
 * <p>
 * Redis 가 아니라 Caffeine 인 이유: 단일 서버 데모에서는 프로세스 안의 맵이면 충분하고,
 * 컨테이너를 하나 덜 띄우는 만큼 남은 일정을 기능에 쓸 수 있다.
 * 서버를 여러 대로 늘릴 때 Redis 로 바꾸면 된다.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String TOUR_PET_DETAIL = "tourPetDetail";

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(TOUR_PET_DETAIL);

        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(10))
                .maximumSize(5_000));

        // 외부 API 가 null 을 돌려준 것도 캐시한다. 없는 장소를 반복 조회하는 것까지 막는다.
        manager.setAllowNullValues(true);

        return manager;
    }
}