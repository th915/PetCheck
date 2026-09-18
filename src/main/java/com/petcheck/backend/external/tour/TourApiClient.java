package com.petcheck.backend.external.tour;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcheck.backend.common.properties.ExternalApiProperties;
import com.petcheck.backend.config.CacheConfig;
import com.petcheck.backend.external.tour.dto.TourPetDetail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * TourAPI 반려동물 동반여행 상세 조회.
 * <p>
 * 이 응답은 DB 에 저장하지 않는다. 장소 상세 화면을 열 때 실시간으로 부르고
 * 10분 캐시로 호출 수를 줄인다. DB 에는 검색과 신뢰도에 필요한 값만 남긴다.
 */
@Slf4j
@Component
public class TourApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ExternalApiProperties.TourApi properties;

    /** 어느 표기로 넣었든 디코딩 형태로 통일해 둔 키 */
    private final String serviceKey;

    public TourApiClient(RestClient tourApiRestClient,
                         ObjectMapper objectMapper,
                         ExternalApiProperties properties) {
        this.restClient = tourApiRestClient;
        this.objectMapper = objectMapper;
        this.properties = properties.tourApi();
        this.serviceKey = normalizeKey(this.properties.key());
    }

    /**
     * 공공데이터포털은 같은 키를 Encoding / Decoding 두 표기로 보여준다.
     * RestClient 의 queryParam() 이 URL 인코딩을 한 번 해주므로,
     * 이미 인코딩된 키를 그대로 넣으면 %2B 가 %252B 로 이중 인코딩돼 실패한다.
     * 어느 쪽을 넣어도 되도록 여기서 디코딩 형태로 맞춘다.
     * <p>
     * '%' 유무로 판별하는 이유: 디코딩 키는 Base64 문자(A-Z a-z 0-9 + / =)뿐이라
     * '%' 가 들어올 수 없다.
     * 풀기 전에 '+' 를 %2B 로 바꾸는 이유: URLDecoder 는 '+' 를 공백으로 바꾼다.
     */
    private String normalizeKey(String key) {
        if (key == null || key.isBlank() || !key.contains("%")) {
            return key;
        }
        return URLDecoder.decode(key.replace("+", "%2B"), StandardCharsets.UTF_8);
    }
    /**
     * @return 응답이 없거나 호출이 실패하면 null.
     *         공공데이터가 없다고 우리 화면이 500 을 내면 안 된다.
     */
    @Cacheable(cacheNames = CacheConfig.TOUR_PET_DETAIL, key = "#contentId")
    public TourPetDetail fetchPetDetail(String contentId) {
        if (!properties.isConfigured()) {
            return null;
        }

        try {
            String body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/" + properties.detailOperation())
                            .queryParam("serviceKey", properties.key())
                            .queryParam("MobileOS", "ETC")
                            .queryParam("MobileApp", "petcheck")
                            .queryParam("_type", "json")
                            .queryParam("contentId", contentId)
                            .build())
                    .retrieve()
                    .body(String.class);

            return parseFirstItem(body);

        } catch (RestClientException e) {
            log.warn("TourAPI 상세 조회 실패 — contentId={}, reason={}", contentId, e.getMessage());
            return null;
        }
    }

    /**
     * 공공데이터포털 공통 응답은 response.body.items.item 안에 실제 데이터가 있다.
     * 결과가 1건이면 객체, 여러 건이면 배열로 내려오는 기관이 많아 둘 다 받는다.
     * 키가 잘못됐거나 한도를 넘기면 JSON 이 아니라 XML 에러가 오므로 그것도 걸러낸다.
     */
    private TourPetDetail parseFirstItem(String body) throws RestClientException {
        if (body == null || body.isBlank() || body.trim().startsWith("<")) {
            log.warn("TourAPI 가 JSON 이 아닌 응답을 돌려줬습니다. 인증키와 일일 한도를 확인하세요.");
            return null;
        }

        try {
            JsonNode item = objectMapper.readTree(body)
                    .path("response").path("body").path("items").path("item");

            if (item.isMissingNode() || item.isNull()) {
                return null;
            }
            JsonNode target = item.isArray() ? item.get(0) : item;

            return target == null ? null : objectMapper.treeToValue(target, TourPetDetail.class);

        } catch (Exception e) {
            log.warn("TourAPI 응답 파싱 실패: {}", e.getMessage());
            return null;
        }
    }
}