package com.petcheck.backend.dto.store;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreRejectRequest(

        @NotBlank(message = "반려 사유는 필수입니다")
        @Size(max = 300)
        String reason
) {
}