package com.penny.exception;

import org.springframework.http.HttpStatus;

public class GeminiException extends ApiException {

    public GeminiException(String message) {
        super(HttpStatus.BAD_GATEWAY, "AI_UNAVAILABLE", message);
    }

    public GeminiException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, "AI_UNAVAILABLE", message, cause);
    }
}
