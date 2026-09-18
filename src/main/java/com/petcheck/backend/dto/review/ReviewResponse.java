package com.petcheck.backend.dto.review;

import com.petcheck.backend.entity.review.AuthenticityStatus;
import com.petcheck.backend.entity.review.MismatchReason;
import com.petcheck.backend.entity.review.Review;
import com.petcheck.backend.entity.review.VerificationLevel;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * @param currentWeight 지금 이 리뷰가 신뢰도 공식에서 몇 표인지.
 *                      v × d × a 를 곱한 값이라, 영수증 리뷰와 오래된 일반 리뷰의
 *                      차이가 숫자로 보인다. 시연에서 이 값이 핵심이다
 */
public record ReviewResponse(
        Long id,
        Long placeId,
        String placeName,
        String writerNickname,
        boolean matched,
        Set<MismatchReason> mismatchReasons,
        List<String> imageUrls,
        LocalDate visitedAt,
        String content,
        VerificationLevel verificationLevel,
        AuthenticityStatus authenticityStatus,
        String authenticityReason,
        double currentWeight,
        LocalDateTime createdAt
) {

    public static ReviewResponse of(Review review, double currentWeight) {
        return new ReviewResponse(
                review.getId(),
                review.getPlace().getId(),
                review.getPlace().getName(),
                review.getMember().getNickname(),
                review.isMatched(),
                review.getMismatchReasons() == null ? Set.of() : Set.copyOf(review.getMismatchReasons()),
                review.getImageUrls() == null ? List.of() : List.copyOf(review.getImageUrls()),
                review.getVisitedAt().toLocalDate(),
                review.getContent(),
                review.getVerification().getLevel(),
                review.getAuthenticity().getStatus(),
                review.getAuthenticity().getReason(),
                currentWeight,
                review.getCreatedAt());
    }
}