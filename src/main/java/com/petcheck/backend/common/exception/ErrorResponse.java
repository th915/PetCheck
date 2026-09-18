package com.petcheck.backend.common.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 모든 에러 응답의 모양. 프론트는 code 로 분기하고 message 를 사용자에게 보여준다.
 *
 * @param fieldErrors 검증 실패 시에만 채워진다. 그 외에는 빈 리스트
 */
public record ErrorResponse(
        String code,
        String message,
        String path,
        LocalDateTime timestamp,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode, String message, String path) {
        return new ErrorResponse(errorCode.name(), message, path, LocalDateTime.now(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(errorCode.name(), errorCode.message(), path,
                LocalDateTime.now(), fieldErrors);
    }
}