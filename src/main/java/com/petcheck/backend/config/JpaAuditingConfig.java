package com.petcheck.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseTimeEntity 의 @CreatedDate / @LastModifiedDate 를 동작시킨다.
 * 메인 클래스가 아니라 별도 설정으로 빼두면 @WebMvcTest 슬라이스에 딸려오지 않는다.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
