package com.petcheck.backend.dto.review;

import java.time.LocalDate;

/**
 * 영수증 대조에 필요한 값만 트랜잭션 안에서 뽑아둔 묶음.
 * Gemini 호출 구간에서는 엔티티를 건드리지 않는다.
 */
public record ReceiptTarget(Long reviewId, String receiptImageUrl, String placeName, LocalDate visitedAt) {
}