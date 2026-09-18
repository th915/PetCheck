package com.petcheck.backend.dto.review;

import com.petcheck.backend.entity.review.AuthenticityStatus;

public record AuthenticityVerdict(AuthenticityStatus status, String reason) {
}