package com.petcheck.backend.dto.review;

import com.petcheck.backend.entity.review.MismatchReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

/**
 * 방문일과 인증 등급은 수정 대상이 아니다.
 * 방문일을 바꿀 수 있으면 오래된 방문을 최신으로 위조할 수 있고,
 * 인증 등급은 작성 시점의 증거로 정해진 값이다.
 */
public record ReviewUpdateRequest(

        @NotNull Boolean matched,
        Set<MismatchReason> mismatchReasons,
        List<String> imageUrls,

        @Size(max = 1000) String content
) {
}