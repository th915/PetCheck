package com.petcheck.backend.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "external.gemini")
public record GeminiProperties(
        String apiKey,
        String baseUrl,
        String model,
        String fallbackModel,
        Duration connectTimeout,
        Duration readTimeout
) {
}