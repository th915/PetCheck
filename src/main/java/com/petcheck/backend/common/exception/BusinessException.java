package com.petcheck.backend.common.exception;

import lombok.Getter;

/**
 * 업무 규칙 위반. 스택트레이스를 만들지 않는다.
 * <p>
 * "장소가 없다"는 버그가 아니라 정상적인 분기다.
 * 예외 하나마다 스택트레이스를 뜨면 응답이 느려지고 로그가 쓸모없어진다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message(), null, false, false);
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detail) {
        super(detail, null, false, false);
        this.errorCode = errorCode;
    }
}