package com.petcheck.backend.controller.admin;

import com.petcheck.backend.dto.common.PageResponse;
import com.petcheck.backend.dto.review.ReviewResponse;
import com.petcheck.backend.dto.review.VerdictOverrideRequest;
import com.petcheck.backend.entity.review.AuthenticityStatus;
import com.petcheck.backend.service.review.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * AI 판정을 사람이 뒤집을 수 있는 통로.
 * 자동 판정만 있고 이의 제기 경로가 없으면, 오판 한 번이 매장에 영구 불이익이 된다.
 */
@RestController
@RequestMapping("/api/admin/reviews")
@RequiredArgsConstructor
public class ReviewModerationController {

    private final ReviewService reviewService;

    @GetMapping
    public PageResponse<ReviewResponse> list(
            @RequestParam(defaultValue = "FAKE") AuthenticityStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.of(reviewService.findByAuthenticityStatus(status, pageable));
    }

    @PostMapping("/{reviewId}/verdict")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void override(@PathVariable Long reviewId,
                         @Valid @RequestBody VerdictOverrideRequest request) {
        reviewService.applyAuthenticity(reviewId, request.status(), 0,
                "관리자 수동 판정: " + request.reason());
    }
}