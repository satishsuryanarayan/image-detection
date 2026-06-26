package com.heb.imagedetection.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Standard error payload returned by {@code GlobalExceptionHandler} for validation, domain, upstream, and generic failures.
 */
@Schema(description = "Standard API error response")
public record ApiErrorResponse(
        @Schema(description = "Time when the error response was generated")
        OffsetDateTime timestamp,
        @Schema(description = "HTTP status code", example = "400")
        int status,
        @Schema(description = "HTTP reason phrase", example = "Bad Request")
        String error,
        @Schema(description = "Human-readable error details", example = "[\"imageUrl: imageUrl must be a valid URL\"]")
        List<String> details
) {
}