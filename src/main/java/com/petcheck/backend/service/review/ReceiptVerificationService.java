package com.petcheck.backend.service.review;

import com.petcheck.backend.common.storage.LocalImageStorage;
import com.petcheck.backend.dto.review.ReceiptRead;
import com.petcheck.backend.dto.review.ReceiptTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;

/**
 * 영수증 대조. Gemini 는 글자를 읽기만 하고, 인증을 줄지는 여기서 정한다.
 * 판단이 코드에 있어야 "왜 인증이 안 됐는지"를 사용자에게 설명할 수 있다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptVerificationService {

    private final ReviewService reviewService;
    private final GeminiClient geminiClient;
    private final LocalImageStorage localImageStorage;

    @Value("${app.review.receipt-date-tolerance-days:1}")
    private long dateTolerance;

    public void verify(Long reviewId) {
        ReceiptTarget target = reviewService.findReceiptTarget(reviewId);
        if (target == null || target.receiptImageUrl() == null) {
            return;     // 영수증을 안 냈으면 할 일이 없다
        }

        ReceiptRead read;
        try {
            read = geminiClient.readReceipt(
                    localImageStorage.read(target.receiptImageUrl()),
                    localImageStorage.mimeTypeOf(target.receiptImageUrl()));
        } catch (Exception e) {
            log.warn("영수증 파일을 읽지 못했다 — reviewId={}, {}", reviewId, e.getMessage());
            return;
        }

        if (!read.readable()) {
            log.info("영수증 판독 실패 — reviewId={}, 인증 등급 유지", reviewId);
            return;
        }

        if (!nameMatches(read.storeName(), target.placeName())) {
            log.info("영수증 상호 불일치 — reviewId={}, 영수증='{}', 장소='{}'",
                    reviewId, read.storeName(), target.placeName());
            return;
        }

        long gap = Math.abs(ChronoUnit.DAYS.between(read.visitDate(), target.visitedAt()));
        if (gap > dateTolerance) {
            log.info("영수증 날짜 불일치 — reviewId={}, 영수증={}, 방문일={} ({}일 차이)",
                    reviewId, read.visitDate(), target.visitedAt(), gap);
            return;
        }

        reviewService.applyReceipt(reviewId, read.storeName(), read.visitDate());
    }

    /**
     * 영수증 상호와 등록된 장소명을 견준다.
     * 공공데이터 상호에는 "(주)", 지점명, 띄어쓰기가 제각각이라
     * 특수문자와 공백을 걷어낸 뒤 한쪽이 다른 쪽을 품고 있으면 같은 곳으로 본다.
     */
    private boolean nameMatches(String receiptName, String placeName) {
        String a = normalize(receiptName);
        String b = normalize(placeName);

        if (a.isBlank() || b.isBlank()) {
            return false;
        }
        return a.contains(b) || b.contains(a);
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^가-힣a-zA-Z0-9]", "").toLowerCase();
    }
}