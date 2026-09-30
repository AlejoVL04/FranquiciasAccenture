package com.example.franchiseapi.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Single error envelope returned by every failing endpoint.
 * <p>
 * {@code validationErrors} is only present for HTTP 400 responses caused by
 * Bean Validation; it is stripped from the payload otherwise
 * ({@code default-property-inclusion: non_null}).
 */
@Schema(name = "ErrorResponse", description = "Standard error payload")
public record ErrorResponse(

        @Schema(description = "When the error was produced", example = "2026-09-30T18:00:00")
        LocalDateTime timestamp,

        @Schema(description = "HTTP status code", example = "404")
        int status,

        @Schema(description = "HTTP reason phrase", example = "Not Found")
        String error,

        @Schema(description = "Human readable explanation", example = "Franchise with id 10 not found")
        String message,

        @Schema(description = "Request path that produced the error", example = "/api/v1/franchises/10")
        String path,

        @Schema(description = "Field-level validation failures, keyed by field name")
        Map<String, String> validationErrors
) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, null);
    }

    public static ErrorResponse validation(int status, String error, String message, String path,
                                           Map<String, String> validationErrors) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, path, validationErrors);
    }
}
