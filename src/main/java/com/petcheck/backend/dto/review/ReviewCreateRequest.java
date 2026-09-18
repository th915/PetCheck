package com.petcheck.backend.dto.review;

import com.petcheck.backend.entity.review.MismatchReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * @param visitedAt  방문한 날. 시각이 아니라 날짜로 받는다 —
 *                   중복 검사를 "같은 날 같은 곳"으로 명확하게 하기 위해서다
 * @param latitude   작성 시점의 브라우저 좌표. 보내면 LOCATION 인증 판정에 쓰이고
 *                   판정 후 버려진다(저장하지 않음). 안 보내도 리뷰는 정상 등록된다
 * @param imageUrls  POST /api/reviews/images 로 먼저 올리고 받은 경로들
 */
public record ReviewCreateRequest(

        @NotNull(message = "장소를 선택해주세요")
        Long placeId,

        @NotNull(message = "등록된 정보와 실제가 일치했는지 선택해주세요")
        Boolean matched,

        Set<MismatchReason> mismatchReasons,

        List<String> imageUrls,

        @NotNull(message = "방문일은 필수입니다")
        @PastOrPresent(message = "미래 날짜는 입력할 수 없습니다")
        LocalDate visitedAt,

        @Size(max = 1000, message = "내용은 1000자 이하여야 합니다")
        String content,

        Double latitude,
        Double longitude,

        /** POST /api/reviews/images 로 올린 영수증 사진 경로. 없으면 일반 리뷰로 처리된다 */
        String receiptImageUrl
) {
}