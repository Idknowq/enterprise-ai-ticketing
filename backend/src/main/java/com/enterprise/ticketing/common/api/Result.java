package com.enterprise.ticketing.common.api;

import com.enterprise.ticketing.common.error.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Result", description = "Unified API response wrapper")
public record Result<T>(
        @Schema(description = "Whether the request succeeded") boolean success,
        @Schema(description = "Stable response code") String code,
        @Schema(description = "Human readable message") String message,
        @Schema(description = "Response payload") T data,
        @Schema(description = "Server time in UTC") Instant timestamp,
        @Schema(description = "Trace id from observability context") String traceId
) {

    public static <T> Result<T> success(T data, String traceId) {
        return new Result<>(true, "COMMON_SUCCESS", "Success", data, Instant.now(), traceId);
    }

    public static Result<Void> success(String traceId) {
        return new Result<>(true, "COMMON_SUCCESS", "Success", null, Instant.now(), traceId);
    }

    public static <T> Result<T> failure(ErrorCode errorCode, String message, T data, String traceId) {
        return new Result<>(false, errorCode.getCode(), message, data, Instant.now(), traceId);
    }

    public static Result<Void> failure(ErrorCode errorCode, String message, String traceId) {
        return failure(errorCode, message, null, traceId);
    }
}

