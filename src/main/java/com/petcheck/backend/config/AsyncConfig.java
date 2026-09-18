package com.petcheck.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 리뷰 진정성 판정 전용 풀.
 * 스레드를 적게 잡은 이유는 Gemini 쪽 분당 요청 제한이 병목이라
 * 여기서 동시에 더 밀어봐야 429 만 늘기 때문이다.
 * 큐가 차면 CallerRunsPolicy 로 호출 스레드에서 처리해 요청을 버리지 않는다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor reviewJudgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("review-judge-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}