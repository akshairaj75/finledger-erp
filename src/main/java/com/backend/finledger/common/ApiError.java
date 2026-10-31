package com.backend.finledger.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Standard error response structure across the FinLedger REST API.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
    @JsonFormat(shape = JsonFormat.Shape.STRING, timezone = "UTC")
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    List<FieldError> fieldErrors
) {
    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, null);
    }

    public static ApiError of(int status, String code, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status, code, message, path, fieldErrors);
    }
}

