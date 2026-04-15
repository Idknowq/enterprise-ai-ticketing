package com.enterprise.ticketing.common.handler;

import com.enterprise.ticketing.common.api.Result;
import com.enterprise.ticketing.common.error.ErrorCode;
import com.enterprise.ticketing.common.exception.BusinessException;
import com.enterprise.ticketing.common.util.TraceIdUtils;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(Result.failure(errorCode, exception.getMessage(), TraceIdUtils.currentTraceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, String>>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.failure(
                        ErrorCode.COMMON_VALIDATION_ERROR,
                        ErrorCode.COMMON_VALIDATION_ERROR.getDefaultMessage(),
                        fieldErrors,
                        TraceIdUtils.currentTraceId()
                ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Map<String, String>>> handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, String> violations = exception.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.failure(
                        ErrorCode.COMMON_VALIDATION_ERROR,
                        ErrorCode.COMMON_VALIDATION_ERROR.getDefaultMessage(),
                        violations,
                        TraceIdUtils.currentTraceId()
                ));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.failure(
                        ErrorCode.COMMON_BAD_REQUEST,
                        "Malformed request body",
                        TraceIdUtils.currentTraceId()
                ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.failure(
                        ErrorCode.COMMON_NOT_FOUND,
                        "Resource not found: " + exception.getResourcePath(),
                        TraceIdUtils.currentTraceId()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.failure(
                        ErrorCode.COMMON_INTERNAL_ERROR,
                        ErrorCode.COMMON_INTERNAL_ERROR.getDefaultMessage(),
                        TraceIdUtils.currentTraceId()
                ));
    }
}
