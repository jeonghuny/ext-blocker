package com.jeonghuny.ext_blocker.policy;

import org.springframework.http.HttpStatus;

public enum PolicyError {
    INVALID_FORMAT(HttpStatus.BAD_REQUEST,
            "확장자는 영문 소문자와 숫자만 사용할 수 있습니다. (최대 20자)"),
    DUPLICATE(HttpStatus.CONFLICT,
            "이미 등록된 확장자입니다."),
    ALREADY_FIXED(HttpStatus.CONFLICT,
            "고정 확장자 목록에 있습니다. 위 체크박스를 이용해 주세요."),
    LIMIT_EXCEEDED(HttpStatus.CONFLICT,
            "커스텀 확장자는 최대 200개까지 등록할 수 있습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND,
            "존재하지 않는 확장자입니다."),
    NOT_FIXED(HttpStatus.BAD_REQUEST,
            "고정 확장자가 아닙니다."),
    CANNOT_DELETE_FIXED(HttpStatus.BAD_REQUEST,
            "고정 확장자는 삭제할 수 없습니다. 체크 해제를 이용해 주세요.");

    private final HttpStatus status;
    private final String message;

    PolicyError(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status()  { return status; }
    public String message()     { return message; }
}