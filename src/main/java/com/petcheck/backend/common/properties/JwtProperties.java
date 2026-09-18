package com.petcheck.backend.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @param secret               HMAC-SHA256 서명 키. 32바이트(문자 32자) 이상이어야 한다
 * @param accessTokenValidity  액세스 토큰 유효기간
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration accessTokenValidity) {

    public JwtProperties {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalArgumentException(
                    "app.jwt.secret 은 32바이트 이상이어야 합니다. openssl rand -base64 48 로 만드세요");
        }
    }
}