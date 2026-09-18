package com.petcheck.backend.common.exception;

import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.List;

/**
 * 컨트롤러에서 빠져나온 예외를 전부 여기서 받는다.
 * 이게 없으면 프론트가 스프링 기본 에러 페이지(HTML)를 받고,
 * JSON 을 기대하던 fetch 가 파싱 단계에서 죽는다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 로컬 개발에서만 true. 예외의 클래스와 메시지를 응답에 담는다.
     * 운영에서 켜면 SQL 문법이나 내부 경로가 그대로 노출되므로 기본값은 false 다.
     */
    @Value("${app.expose-error-detail:false}")
    private boolean exposeErrorDetail;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();

        // 업무 예외는 WARN. 서버가 고장난 게 아니라 요청이 규칙에 안 맞은 것이다.
        log.warn("업무 예외 — {} {} : {}", request.getMethod(), request.getRequestURI(), e.getMessage());

        return ResponseEntity
                .status(errorCode.status())
                .body(ErrorResponse.of(errorCode, e.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e,
                                                          HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.status())
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, request.getRequestURI(), fieldErrors));
    }

    /**
     * 경로 변수나 쿼리 파라미터의 타입 변환 실패.
     * 예) /api/store/me 에서 "me" 를 Long storeId 로 바꾸려다 실패
     * <p>
     * 잘못된 요청이지 서버 고장이 아니므로 500 이 아니라 400 이다.
     * 마지막 Exception 핸들러로 떨어지면 원인이 "서버 오류"로 뭉개진다.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                            HttpServletRequest request) {
        String message = "%s 값이 올바르지 않습니다: %s".formatted(e.getName(), e.getValue());
        log.warn("타입 변환 실패 — {} {} : {}", request.getMethod(), request.getRequestURI(), message);

        return ResponseEntity
                .status(ErrorCode.INVALID_REQUEST.status())
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST, message, request.getRequestURI()));
    }

    /**
     * 본문 JSON 을 읽지 못한 경우 — 날짜 형식 오류, 잘못된 enum 값, 깨진 JSON 등.
     * 클라이언트 잘못이므로 400 으로 내린다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException e,
                                                           HttpServletRequest request) {
        log.warn("본문 파싱 실패 — {}", describe(e));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.INVALID_REQUEST,
                        "요청 본문의 형식이 올바르지 않습니다", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e, HttpServletRequest request) {
        // 예상 못 한 예외만 ERROR + 스택트레이스. 이건 진짜 봐야 하는 로그다.
        log.error("처리되지 않은 예외 — {} {}", request.getMethod(), request.getRequestURI(), e);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.status())
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, describe(e), request.getRequestURI()));
    }

    /**
     * 원인 사슬의 가장 안쪽 예외까지 따라간다.
     * 스프링이 감싸서 던지는 경우가 많아, 껍데기만 보면
     * "InvocationTargetException" 같은 쓸모없는 이름만 남는다.
     */
    private String describe(Exception e) {
        if (!exposeErrorDetail) {
            return ErrorCode.INTERNAL_ERROR.message();
        }

        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getName() + ": " + root.getMessage();
    }
}