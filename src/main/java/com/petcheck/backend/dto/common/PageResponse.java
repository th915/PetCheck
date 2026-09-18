package com.petcheck.backend.dto.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Page 를 그대로 반환하지 않고 한 겹 감싼다.
 * <p>
 * Spring 의 PageImpl 은 직렬화 형태가 버전에 따라 바뀔 수 있어서
 * 그대로 내보내면 프론트가 의존하는 응답 구조가 우리 통제를 벗어난다.
 * (Spring Boot 3.3 부터는 PageImpl 직접 직렬화에 경고가 뜬다)
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}