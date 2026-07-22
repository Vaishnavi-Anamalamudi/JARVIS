package com.adaptivegateway.common.exception;

import com.adaptivegateway.common.api.ErrorField;
import com.adaptivegateway.common.api.ErrorResponse;
import com.adaptivegateway.common.web.CorrelationIdFilter;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;

@Order(-2)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException exception,
            ServerWebExchange exchange
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("Business exception handled: code={}, path={}", errorCode.code(), path(exchange));
        return build(exchange, errorCode, exception.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            ServerWebExchange exchange
    ) {
        List<ErrorField> fields = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toErrorField)
                .toList();
        return build(exchange, ErrorCode.VALIDATION_FAILED, "Request validation failed", fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(
            HandlerMethodValidationException exception,
            ServerWebExchange exchange
    ) {
        List<ErrorField> fields = exception.getParameterValidationResults()
                .stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new ErrorField(result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return build(exchange, ErrorCode.VALIDATION_FAILED, "Request validation failed", fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            ServerWebExchange exchange
    ) {
        List<ErrorField> fields = exception.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorField(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        return build(exchange, ErrorCode.VALIDATION_FAILED, "Request validation failed", fields);
    }

    @ExceptionHandler({ServerWebInputException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleInvalidBody(Exception exception, ServerWebExchange exchange) {
        log.warn("Invalid request body: path={}, message={}", path(exchange), exception.getMessage());
        return build(exchange, ErrorCode.REQUEST_BODY_INVALID, "Request body is invalid", List.of());
    }

    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            UnsupportedMediaTypeStatusException exception,
            ServerWebExchange exchange
    ) {
        return build(exchange, ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Content type is not supported", List.of());
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException exception, ServerWebExchange exchange) {
        log.error("Database access failure: path={}", path(exchange), exception);
        return build(exchange, ErrorCode.DATABASE_UNAVAILABLE, "Database operation failed", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception, ServerWebExchange exchange) {
        log.error("Unhandled exception: path={}", path(exchange), exception);
        return build(exchange, ErrorCode.INTERNAL_ERROR, "Unexpected server error", List.of());
    }

    private ErrorField toErrorField(FieldError fieldError) {
        return new ErrorField(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private ResponseEntity<ErrorResponse> build(
            ServerWebExchange exchange,
            ErrorCode errorCode,
            String message,
            List<ErrorField> fields
    ) {
        ErrorResponse response = ErrorResponse.of(
                correlationId(exchange),
                errorCode.code(),
                message,
                path(exchange),
                fields
        );
        return ResponseEntity.status(errorCode.httpStatus()).body(response);
    }

    private String correlationId(ServerWebExchange exchange) {
        Object value = exchange.getAttribute(CorrelationIdFilter.CORRELATION_ID_ATTRIBUTE);
        return value == null ? null : value.toString();
    }

    private String path(ServerWebExchange exchange) {
        return exchange.getRequest().getPath().value();
    }
}
