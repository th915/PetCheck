package com.petcheck.backend.entity.review;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 방문 인증 결과.
 * <p>
 * 좌표 자체는 저장하지 않는다. 판정에만 쓰고 결과(등급)만 남긴다 —
 * 리뷰 작성자의 위치 기록을 계속 들고 있을 이유가 없다.
 * 영수증도 금액은 버리고 대조에 쓴 상호명·날짜만 남긴다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewVerification {

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_level", nullable = false, length = 20)
    private VerificationLevel level;

    /** 영수증에서 읽어낸 상호명. RECEIPT 등급일 때만 채워진다 */
    @Column(name = "receipt_store_name", length = 200)
    private String receiptStoreName;

    @Column(name = "receipt_date")
    private LocalDate receiptDate;

    private ReviewVerification(VerificationLevel level, String receiptStoreName, LocalDate receiptDate) {
        this.level = level;
        this.receiptStoreName = receiptStoreName;
        this.receiptDate = receiptDate;
    }

    public static ReviewVerification of(VerificationLevel level) {
        return new ReviewVerification(level, null, null);
    }

    public static ReviewVerification byReceipt(String storeName, LocalDate date) {
        return new ReviewVerification(VerificationLevel.RECEIPT, storeName, date);
    }
}