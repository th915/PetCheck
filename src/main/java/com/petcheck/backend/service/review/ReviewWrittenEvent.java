package com.petcheck.backend.service.review;

/** 리뷰가 커밋된 뒤 진정성 판정을 트리거하는 이벤트 */
public record ReviewWrittenEvent(Long reviewId) {
}