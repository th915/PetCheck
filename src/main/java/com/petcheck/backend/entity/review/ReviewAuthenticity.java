package com.petcheck.backend.entity.review;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * AI 진정성 판정 결과.
 * <p>
 * 근거(reason)를 반드시 남긴다. "AI가 그랬다"로 끝나면 오탐이 났을 때
 * 관리자가 복구 판단을 할 수 없다.
 * ruleScore 는 규칙 기반 필터의 합산 점수로, LLM 없이도 설명 가능한 부분이다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewAuthenticity {

    @Enumerated(EnumType.STRING)
    @Column(name = "authenticity_status", nullable = false, length = 20)
    private AuthenticityStatus status;

    @Column(name = "rule_score", nullable = false)
    private int ruleScore;

    @Column(name = "authenticity_reason", length = 1000)
    private String reason;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    private ReviewAuthenticity(AuthenticityStatus status, int ruleScore,
                               String reason, LocalDateTime analyzedAt) {
        this.status = status;
        this.ruleScore = ruleScore;
        this.reason = reason;
        this.analyzedAt = analyzedAt;
    }

    /** 작성 직후. 아직 분석 전이다 */
    public static ReviewAuthenticity pending() {
        return new ReviewAuthenticity(AuthenticityStatus.PENDING, 0, null, null);
    }

    public static ReviewAuthenticity of(AuthenticityStatus status, int ruleScore, String reason) {
        return new ReviewAuthenticity(status, ruleScore, reason, LocalDateTime.now());
    }
}