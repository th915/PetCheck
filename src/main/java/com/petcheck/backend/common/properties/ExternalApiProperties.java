package com.petcheck.backend.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * external.* 설정을 통째로 받는다.
 * <p>
 * @Value 를 여기저기 흩뿌리지 않는 이유: 키가 비어 있는지,
 * 타임아웃이 말이 되는지를 기동 시점에 한 번에 확인하기 위해서다.
 * 중첩 record 라 external.tour-api.base-url 이 그대로 tourApi().baseUrl() 이 된다.
 */
@ConfigurationProperties(prefix = "external")
public record ExternalApiProperties(
        TourApi tourApi,
        CultureData cultureData,
        FoodSafety foodSafety,
        Law law,
        KakaoLocal kakaoLocal
) {

    public record TourApi(
            String key,
            String baseUrl,
            String detailOperation,
            Duration connectTimeout,
            Duration readTimeout
    ) {
        public boolean isConfigured() {
            return key != null && !key.isBlank();
        }
    }

    public record CultureData(String csvPath, String charset) {
    }

    /** 식품안전나라는 OpenAPI 가 아니라 xlsx 다운로드라 인증키가 없다. */
    public record FoodSafety(String xlsxPath) {
    }

    /** 법제처는 인증키가 아니라 신청 이메일의 ID 를 OC 파라미터로 쓴다. */
    public record Law(String oc, String baseUrl) {

        public boolean isConfigured() {
            return oc != null && !oc.isBlank();
        }
    }

    /**
     * 카카오 로컬 키워드 검색. DB 에 없는 장소도 검색·즐겨찾기·일정·리뷰가 되게 하는
     * "장소 실체화"(PlaceMaterializationService) 의 데이터 소스다.
     * 앱 키(kakao.map.js-key)와는 다른 REST API 키를 쓴다.
     */
    public record KakaoLocal(
            String key,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout
    ) {
        public boolean isConfigured() {
            return key != null && !key.isBlank();
        }
    }
}