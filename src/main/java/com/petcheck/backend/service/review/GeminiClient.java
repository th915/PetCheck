package com.petcheck.backend.service.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petcheck.backend.common.properties.GeminiProperties;
import com.petcheck.backend.dto.review.AuthenticityVerdict;
import com.petcheck.backend.dto.review.ReceiptRead;
import com.petcheck.backend.dto.review.ReviewInspection;
import com.petcheck.backend.entity.review.AuthenticityStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Gemini 호출 담당. 텍스트 판정과 영수증 판독 두 가지를 쓴다.
 * 실패하면 예외를 던지지 않고 "판정 못 함"에 해당하는 값을 돌려준다.
 * API 장애가 곧 리뷰 불이익이 되면 안 되기 때문이다.
 */
@Slf4j
@Component
public class GeminiClient {

    private static final String JUDGE_PROMPT = """
            너는 반려동물 동반 가능 장소 리뷰의 진정성을 검수하는 심사자다.
            아래 리뷰가 실제 방문에 근거한 리뷰인지 판단해라.

            [판정 기준]
            1. 광고·홍보 목적의 문구가 있는가
            2. 대가를 받고 쓴 정황이 있는가
            3. 작성자가 고른 "정보 일치 여부"와 본문 내용이 서로 모순되는가
            4. 어디에나 복사해 붙일 수 있는 말뿐이고, 그 장소에서 실제로 겪었을 법한
               구체적인 사실이 하나도 없는가

            [판정 규칙]
            - 기준 1 또는 2 에 해당하면 FAKE
            - 기준 3 또는 4 가 본문 안에서 분명히 드러나면 SUSPICIOUS
            - 그 밖에는 모두 GENUINE. 판단이 서지 않으면 GENUINE 으로 한다

            [감점하면 안 되는 것]
            - 업종과 본문 내용이 어울리지 않는다는 이유로 감점하지 마라.
              약국에도 대기 의자가 있고, 미용실에도 좌석이 있다.
            - 맞춤법이 틀리거나 문장이 거칠거나 짧다는 이유로 감점하지 마라.
            - 매장에 불리한 내용이라는 이유로 감점하지 마라. 불만도 진짜 리뷰다.
            - 본문에 없는 사실을 추측해서 판정 근거로 삼지 마라.

            [검수 대상]
            장소: %s
            작성자가 선택한 정보 일치 여부: %s
            본문: %s

            reason 은 한국어 한 문장으로, 본문에서 확인되는 근거만 적어라.
            """;

    private static final String RECEIPT_PROMPT = """
            이 이미지는 영수증 사진이다. 아래 두 가지만 읽어라.

            1. 상호명 — 영수증 상단의 가게 이름. 지점명이 붙어 있으면 그대로 포함한다
            2. 거래 일자 — yyyy-MM-dd 형식(날짜중간 -는 .과 _으로 대체될 수 있다)

            금액, 카드번호, 결제수단, 구매 품목은 읽지 마라. 쓰지 않는 정보다.
            영수증이 아니거나 글자를 읽을 수 없으면 readable 을 false 로 하고
            나머지는 빈 문자열로 둬라. 추측해서 채우지 마라.
            """;

    private static final Map<String, Object> JUDGE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "status", Map.of("type", "STRING",
                            "enum", List.of("GENUINE", "SUSPICIOUS", "FAKE")),
                    "reason", Map.of("type", "STRING")),
            "required", List.of("status", "reason"));

    private static final Map<String, Object> RECEIPT_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "readable", Map.of("type", "BOOLEAN"),
                    "storeName", Map.of("type", "STRING"),
                    "visitDate", Map.of("type", "STRING")),
            "required", List.of("readable", "storeName", "visitDate"));

    private final RestClient restClient;
    private final GeminiProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiClient(GeminiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) properties.connectTimeout().toMillis());
        factory.setReadTimeout((int) properties.readTimeout().toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(factory)
                .defaultHeader("x-goog-api-key", properties.apiKey())
                .build();
    }

    public AuthenticityVerdict judge(ReviewInspection inspection) {
        String prompt = JUDGE_PROMPT.formatted(
                inspection.placeName(),
                inspection.matched() ? "일치" : "불일치",
                inspection.content());

        JsonNode result = generate(List.of(Map.of("text", prompt)), JUDGE_SCHEMA);

        if (result == null) {
            return new AuthenticityVerdict(AuthenticityStatus.GENUINE, "AI 판정 실패 — 기본 통과 처리");
        }
        return new AuthenticityVerdict(
                AuthenticityStatus.valueOf(result.path("status").asText()),
                result.path("reason").asText());
    }

    public ReceiptRead readReceipt(byte[] image, String mimeType) {
        JsonNode result = generate(List.of(
                        Map.of("text", RECEIPT_PROMPT),
                        Map.of("inline_data", Map.of(
                                "mime_type", mimeType,
                                "data", Base64.getEncoder().encodeToString(image)))),
                RECEIPT_SCHEMA);

        if (result == null) {
            log.warn("영수증 판독 — Gemini 호출이 실패했다. 바로 위 'Gemini ... 실패' 로그를 볼 것");
            return new ReceiptRead(false, null, null);
        }
        if (!result.path("readable").asBoolean()) {
            log.info("영수증 판독 — 모델이 글자를 읽지 못했다. 응답={}, 보낸 형식={}, 크기={}KB",
                    result, mimeType, image.length / 1024);
            return new ReceiptRead(false, null, null);
        }

        try {
            return new ReceiptRead(true,
                    result.path("storeName").asText(),
                    LocalDate.parse(result.path("visitDate").asText()));
        } catch (Exception e) {
            log.warn("영수증 날짜 파싱 실패 — {}", result.path("visitDate").asText());
            return new ReceiptRead(false, null, null);
        }
    }

    /** 기본 모델 → 폴백 모델 순으로 시도하고, 둘 다 실패하면 null 을 준다 */
    private JsonNode generate(List<Map<String, Object>> parts, Map<String, Object> schema) {
        try {
            return call(properties.model(), parts, schema);
        } catch (Exception first) {
            log.warn("Gemini 기본 모델 실패 — {} / {}", properties.model(), first.getMessage());
        }

        try {
            return call(properties.fallbackModel(), parts, schema);
        } catch (Exception second) {
            log.warn("Gemini 폴백 모델도 실패 — {} / {}", properties.fallbackModel(), second.getMessage());
            return null;
        }
    }

    private JsonNode call(String model, List<Map<String, Object>> parts,
                          Map<String, Object> schema) throws Exception {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", parts)),
                "generationConfig", Map.of(
                        "temperature", 0,
                        "responseMimeType", "application/json",
                        "responseSchema", schema));

        String raw = restClient.post()
                .uri("/models/{model}:generateContent", model)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode text = objectMapper.readTree(raw)
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text");

        if (text.isMissingNode()) {
            throw new IllegalStateException("응답에 결과가 없음: " + raw);
        }
        return objectMapper.readTree(text.asText());
    }
}