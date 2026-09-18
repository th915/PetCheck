package com.petcheck.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 에러를 한곳에 모아둔다.
 * 코드 문자열과 상태 코드가 흩어져 있으면 프론트가 분기할 기준이 사라진다.
 */
public enum ErrorCode {

    PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 장소입니다"),
    STORE_NOT_FOUND(HttpStatus.NOT_FOUND, "등록된 매장 정보가 없습니다"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다"),

    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),
    STORE_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 등록된 장소입니다"),
    NOT_STORE_OWNER(HttpStatus.FORBIDDEN, "본인이 등록한 매장이 아닙니다"),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "파일이 비어 있습니다"),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "이미지 파일만 업로드할 수 있습니다"),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 저장에 실패했습니다"),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 리뷰입니다"),
    REVIEW_ALREADY_WRITTEN(HttpStatus.CONFLICT, "같은 방문일로 이미 리뷰를 작성했습니다"),
    NOT_REVIEW_OWNER(HttpStatus.FORBIDDEN, "본인이 작성한 리뷰가 아닙니다"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다"),
    SYNC_SOURCE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "공공데이터 원본을 읽을 수 없습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),
    BUSINESS_NUMBER_INVALID(HttpStatus.BAD_REQUEST, "사업자등록번호 형식이 올바르지 않습니다"),
    BUSINESS_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "정상 영업 중인 사업자가 아닙니다"),
    BUSINESS_NUMBER_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 사업자등록번호입니다"),
    STORE_NAME_DUPLICATED(HttpStatus.CONFLICT, "같은 이름의 매장이 근처에 이미 등록되어 있습니다");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}