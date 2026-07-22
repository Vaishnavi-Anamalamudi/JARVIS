package com.adaptivegateway.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_FAILED("AGW-400-001", HttpStatus.BAD_REQUEST),
    REQUEST_BODY_INVALID("AGW-400-002", HttpStatus.BAD_REQUEST),
    AUTHENTICATION_FAILED("AGW-401-001", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID("AGW-401-002", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AGW-403-001", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND("AGW-404-001", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("AGW-405-001", HttpStatus.METHOD_NOT_ALLOWED),
    RESOURCE_CONFLICT("AGW-409-001", HttpStatus.CONFLICT),
    RATE_LIMIT_EXCEEDED("AGW-429-001", HttpStatus.TOO_MANY_REQUESTS),
    UNSUPPORTED_MEDIA_TYPE("AGW-415-001", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    BUSINESS_RULE_VIOLATION("AGW-422-001", HttpStatus.UNPROCESSABLE_CONTENT),
    INTERNAL_ERROR("AGW-500-001", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_UNAVAILABLE("AGW-503-001", HttpStatus.SERVICE_UNAVAILABLE),
    REDIS_UNAVAILABLE("AGW-503-002", HttpStatus.SERVICE_UNAVAILABLE),
    KAFKA_UNAVAILABLE("AGW-503-003", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final HttpStatus httpStatus;

    ErrorCode(String code, HttpStatus httpStatus) {
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public String code() {
        return code;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
