package com.petcheck.backend.controller.review;

import com.petcheck.backend.common.security.MemberPrincipal;
import com.petcheck.backend.dto.common.PageResponse;
import com.petcheck.backend.dto.review.ReviewCreateRequest;
import com.petcheck.backend.dto.review.ReviewResponse;
import com.petcheck.backend.dto.review.ReviewUpdateRequest;
import com.petcheck.backend.dto.store.ImageUploadResponse;
import com.petcheck.backend.service.review.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 리뷰는 고객만 쓴다. SecurityConfig 에서 목록 조회(GET /api/reviews)만 공개하고
 * 나머지 /api/reviews/** 는 CUSTOMER 로 막는다.
 * 매장 사장님이 자기 매장에 리뷰를 쓰는 경로가 구조적으로 없다.
 */
@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 장소별 리뷰 목록. 공개 */
    @GetMapping
    public PageResponse<ReviewResponse> findByPlace(@RequestParam Long placeId,
                                                    @PageableDefault(size = 10) Pageable pageable) {
        return PageResponse.of(reviewService.findByPlace(placeId, pageable));
    }

    @GetMapping("/me")
    public PageResponse<ReviewResponse> myReviews(@AuthenticationPrincipal MemberPrincipal principal,
                                                  @PageableDefault(size = 10) Pageable pageable) {
        return PageResponse.of(reviewService.findMyReviews(principal.id(), pageable));
    }

    /** 사진을 먼저 올리고, 반환된 url 을 작성 요청의 imageUrls 에 넣는다 */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadResponse uploadImage(@RequestPart("file") MultipartFile file) {
        return new ImageUploadResponse(reviewService.uploadImage(file));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse create(@AuthenticationPrincipal MemberPrincipal principal,
                                 @Valid @RequestBody ReviewCreateRequest request,
                                 HttpServletRequest servletRequest) {
        return reviewService.create(principal.id(), request, clientIp(servletRequest));
    }

    @PutMapping("/{reviewId}")
    public ReviewResponse update(@AuthenticationPrincipal MemberPrincipal principal,
                                 @PathVariable Long reviewId,
                                 @Valid @RequestBody ReviewUpdateRequest request) {
        return reviewService.update(principal.id(), reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal MemberPrincipal principal,
                       @PathVariable Long reviewId) {
        reviewService.delete(principal.id(), reviewId);
    }

    /**
     * 프록시를 거치면 getRemoteAddr() 이 프록시 주소를 준다.
     * X-Forwarded-For 는 쉼표로 이어지므로 맨 앞(최초 클라이언트)만 쓴다.
     */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        return forwarded.split(",")[0].trim();
    }
}