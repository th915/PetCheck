package com.petcheck.backend.external.nts;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcheck.backend.common.properties.NtsProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 국세청 사업자등록 상태조회.
 * <p>
 * 형식 검증(체크섬)을 먼저 로컬에서 돌려서, 자릿수만 맞춘 아무 숫자로
 * 외부 API 를 두드리는 일이 없게 한다.
 */
@Slf4j
@Component
public class BusinessRegistryClient {

    /** 국세청 사업자등록번호 검증 알고리즘의 자리별 가중치 */
    private static final int[] WEIGHTS = {1, 3, 7, 1, 3, 7, 1, 3, 5};

    private final RestClient restClient;
    private final NtsProperties properties;
    private final ObjectMapper objectMapper;

    public BusinessRegistryClient(NtsProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .build();
    }

    /** 하이픈과 공백을 걷어낸 10자리 숫자 */
    public static String normalize(String businessNumber) {
        return businessNumber == null ? "" : businessNumber.replaceAll("[^0-9]", "");
    }

    /**
     * 체크섬 검증. 국세청에 물어보기 전에 형식부터 거른다.
     * 마지막 자리가 앞 아홉 자리로부터 계산되는 검증 숫자다.
     */
    public static boolean hasValidFormat(String normalized) {
        if (normalized.length() != 10) {
            return false;
        }

        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += (normalized.charAt(i) - '0') * WEIGHTS[i];
        }
        sum += ((normalized.charAt(8) - '0') * 5) / 10;

        int checkDigit = (10 - (sum % 10)) % 10;
        return checkDigit == (normalized.charAt(9) - '0');
    }

    /**
     * 국세청에 현재 상태를 묻는다. 조회 자체가 실패하면 null 을 준다.
     * 외부 API 장애로 멀쩡한 사장님의 등록이 막히면 안 되므로,
     * 호출하는 쪽에서 통과시킬지 막을지 정한다.
     */
    public BusinessStatus fetchStatus(String normalized) {
        try {
            String raw = restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/status")
                            .queryParam("serviceKey", properties.key())
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of("b_no", List.of(normalized)))
                    .retrieve()
                    .body(String.class);

            JsonNode item = objectMapper.readTree(raw).path("data").path(0);
            if (item.isMissingNode()) {
                log.warn("국세청 응답에 data 가 없다 — {}", raw);
                return null;
            }

            // 문서와 예제에 b_stt 와 b_stat 두 표기가 섞여 있어 둘 다 본다
            String code = item.hasNonNull("b_stt_cd")
                    ? item.path("b_stt_cd").asText()
                    : item.path("b_stat_cd").asText();
            String name = item.hasNonNull("b_stt")
                    ? item.path("b_stt").asText()
                    : item.path("b_stat").asText();

            return new BusinessStatus(normalized, code, name, item.path("end_dt").asText());

        } catch (Exception e) {
            log.warn("국세청 조회 실패 — {} / {}", normalized, e.getMessage());
            return null;
        }
    }
}