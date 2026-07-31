package com.jeonghuny.ext_blocker.validation;

public enum RejectReason {
    EMPTY("파일명이 비어 있습니다."),
    TOO_LONG("파일명이 너무 깁니다."),
    PATH_TRAVERSAL("파일명에 경로 문자가 포함되어 있습니다."),
    CONTROL_CHARACTER("파일명에 허용되지 않는 제어 문자가 포함되어 있습니다."),
    BLOCKED_EXTENSION("차단된 확장자입니다.");

    private final String message;

    RejectReason(String message) { this.message = message; }

    public String message() { return message; }
}