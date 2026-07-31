package com.jeonghuny.ext_blocker;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class ProbeExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Map<String, Object> tooLarge(MaxUploadSizeExceededException e) {
        return Map.of(
                "error", "PAYLOAD_TOO_LARGE",
                "source", "application",      // ← 이 필드가 측정의 핵심
                "message", "허용 크기를 초과했습니다."
        );
    }
}