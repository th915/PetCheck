package com.petcheck.backend.dto.review;

import com.petcheck.backend.entity.review.AuthenticityStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerdictOverrideRequest(

        @NotNull(message = "판정 결과는 필수입니다")
        AuthenticityStatus status,

        @NotBlank(message = "판정 사유를 남겨주세요")
        String reason
) {
}