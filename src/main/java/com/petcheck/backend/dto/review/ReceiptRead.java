package com.petcheck.backend.dto.review;

import java.time.LocalDate;

/**
 * 영수증 이미지에서 뽑아낸 값. 판정이 아니라 판독 결과다.
 *
 * @param readable 영수증이 아니거나 글자를 읽지 못한 경우 false
 */
public record ReceiptRead(boolean readable, String storeName, LocalDate visitDate) {
}