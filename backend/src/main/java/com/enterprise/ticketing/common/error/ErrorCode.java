package com.enterprise.ticketing.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    COMMON_SUCCESS("COMMON_SUCCESS", HttpStatus.OK, "Success"),
    COMMON_BAD_REQUEST("COMMON_BAD_REQUEST", HttpStatus.BAD_REQUEST, "Bad request"),
    COMMON_VALIDATION_ERROR("COMMON_VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Validation failed"),
    COMMON_UNAUTHORIZED("COMMON_UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Unauthorized"),
    COMMON_FORBIDDEN("COMMON_FORBIDDEN", HttpStatus.FORBIDDEN, "Forbidden"),
    COMMON_NOT_FOUND("COMMON_NOT_FOUND", HttpStatus.NOT_FOUND, "Resource not found"),
    COMMON_CONFLICT("COMMON_CONFLICT", HttpStatus.CONFLICT, "Conflict"),
    COMMON_NOT_IMPLEMENTED("COMMON_NOT_IMPLEMENTED", HttpStatus.NOT_IMPLEMENTED, "Capability not implemented"),
    COMMON_INTERNAL_ERROR("COMMON_INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    COMMON_SERVICE_UNAVAILABLE("COMMON_SERVICE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    AUTH_USER_DISABLED("AUTH_USER_DISABLED", HttpStatus.FORBIDDEN, "User is disabled"),
    AUTH_INVALID_TOKEN("AUTH_INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Invalid JWT token"),
    AUTH_TOKEN_EXPIRED("AUTH_TOKEN_EXPIRED", HttpStatus.UNAUTHORIZED, "JWT token has expired"),
    KNOWLEDGE_UNSUPPORTED_FILE("KNOWLEDGE_UNSUPPORTED_FILE", HttpStatus.BAD_REQUEST, "Unsupported knowledge file"),
    KNOWLEDGE_DOCUMENT_PROCESSING_FAILED("KNOWLEDGE_DOCUMENT_PROCESSING_FAILED", HttpStatus.BAD_REQUEST, "Failed to process knowledge document"),
    KNOWLEDGE_RETRIEVAL_FAILED("KNOWLEDGE_RETRIEVAL_FAILED", HttpStatus.SERVICE_UNAVAILABLE, "Knowledge retrieval failed"),
    KNOWLEDGE_VECTOR_STORE_UNAVAILABLE("KNOWLEDGE_VECTOR_STORE_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE, "Knowledge vector store unavailable");

    private final String code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(String code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
