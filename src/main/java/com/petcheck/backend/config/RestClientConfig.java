package com.petcheck.backend.config;

import com.petcheck.backend.common.properties.ExternalApiProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 공공데이터 호출 전용 RestClient.
 * <p>
 * 타임아웃을 반드시 준다. 기본값은 무제한이라 공공데이터포털이 느려지면
 * 우리 스레드가 그대로 묶이고, 사용자 요청 스레드까지 같이 마른다.
 * 실시간 호출 구조에서는 이게 곧 장애다.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient tourApiRestClient(ExternalApiProperties properties) {
        ExternalApiProperties.TourApi tourApi = properties.tourApi();

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(tourApi.connectTimeout())
                .withReadTimeout(tourApi.readTimeout());

        return RestClient.builder()
                .baseUrl(tourApi.baseUrl())
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
    }
}