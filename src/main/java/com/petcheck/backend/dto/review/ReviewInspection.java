package com.petcheck.backend.dto.review;

/**
 * 룰 검사 결과 + Gemini 프롬프트를 만드는 데 필요한 값 묶음.
 * 트랜잭션 안에서 한 번에 뽑아두기 때문에, 이후 LLM 호출 구간에서는
 * 엔티티나 세션을 전혀 건드리지 않는다.
 *
 * @param ruleScore 규칙 위반 점수 합. 높을수록 조작 가능성이 크다
 * @param ruleReason 어떤 규칙에 걸렸는지 사람이 읽을 수 있는 설명
 */
public record ReviewInspection(
        Long reviewId,
        int ruleScore,
        String ruleReason,
        String content,
        boolean matched,
        String placeName
) {
}