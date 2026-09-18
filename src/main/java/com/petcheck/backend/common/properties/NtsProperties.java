package com.petcheck.backend.common.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.nts")
public record NtsProperties(String key, String baseUrl) {
}