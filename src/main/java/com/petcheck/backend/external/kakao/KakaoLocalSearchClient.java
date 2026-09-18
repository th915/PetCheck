package com.petcheck.backend.external.kakao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcheck.backend.common.properties.ExternalApiProperties;
import com.petcheck.backend.external.kakao.dto.KakaoPlaceResult;
import com.petcheck.backend.external.support.CategoryMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.List;

/**
 * 카카오맵에 있지만 우리 DB(공공데이터 4종)에는 없는 장소를 찾는다.
 * <p>
 * 이 클라이언트는 아무것도 저장하지 않는다. 검색 결과를 그대로 보여주고,
 * 사용자가 실제로 상호작용(즐겨찾기·일정·리뷰)할 때만
 * PlaceMaterializationService 가 Place 로 만든다 — 스치듯 본 15만 건을
 * 전부 저장하면 데이터베이스가 검색으로 오염된다.
 */
@Slf4j
@Component
public class KakaoLocalSearchClient {

    private static final int MAX_RESULTS = 15;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final ExternalApiProperties.KakaoLocal properties;

    public KakaoLocalSearchClient(RestClient kakaoLocalRestClient, ObjectMapper objectMapper,
                                   ExternalApiProperties properties) {
        this.restClient = kakaoLocalRestClient;
        this.objectMapper = objectMapper;
        this.properties = properties.kakaoLocal();
    }

    /**
     * @param latitude  주어지면 이 좌표 근처부터(radius 20km) 정렬해 받는다.
     * @return 실패하거나 키가 없으면 빈 리스트. 검색 실패가 지도 화면을 막으면 안 된다.
     */
    public List<KakaoPlaceResult> search(String keyword, Double latitude, Double longitude) {
        if (!properties.isConfigured() || keyword == null || keyword.isBlank()) {
            return List.of();
        }

        try {
            String body = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/search/keyword.json")
                                .queryParam("query", keyword)
                                .queryParam("size", MAX_RESULTS);
                        if (latitude != null && longitude != null) {
                            uriBuilder.queryParam("x", longitude)
                                    .queryParam("y", latitude)
                                    .queryParam("radius", 20000)
                                    .queryParam("sort", "distance");
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(String.class);

            return parseResults(body);

        } catch (RestClientException e) {
            log.warn("카카오 로컬 검색 실패 — keyword={}, reason={}", keyword, e.getMessage());
            return List.of();
        }
    }

    private List<KakaoPlaceResult> parseResults(String body) {
        List<KakaoPlaceResult> results = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return results;
        }

        try {
            JsonNode documents = objectMapper.readTree(body).path("documents");
            for (JsonNode doc : documents) {
                String lat = doc.path("y").asText(null);
                String lng = doc.path("x").asText(null);
                if (lat == null || lng == null) {
                    continue;
                }
                String address = doc.path("road_address_name").asText("");
                if (address.isBlank()) {
                    address = doc.path("address_name").asText("");
                }

                results.add(new KakaoPlaceResult(
                        doc.path("id").asText(),
                        doc.path("place_name").asText(""),
                        address,
                        doc.path("phone").asText(null),
                        Double.parseDouble(lat),
                        Double.parseDouble(lng),
                        CategoryMapper.fromKakaoCategoryName(doc.path("category_name").asText(""))
                ));
            }
        } catch (Exception e) {
            log.warn("카카오 로컬 검색 응답 파싱 실패: {}", e.getMessage());
        }
        return results;
    }
}
