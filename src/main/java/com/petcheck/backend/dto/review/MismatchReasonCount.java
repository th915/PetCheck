package com.petcheck.backend.dto.review;

import com.petcheck.backend.entity.review.MismatchReason;

/**
 * "어떤 정보가 가장 자주 틀리는가" 집계 한 줄.
 * 관리자 대시보드에서 개선 우선순위를 정하는 근거가 된다.
 */
public record MismatchReasonCount(MismatchReason reason, Long count) {
}
