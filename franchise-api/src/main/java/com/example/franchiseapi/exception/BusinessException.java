package com.example.franchiseapi.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a request is well-formed but violates a business rule.
 * <p>
 * The status is carried by the exception so the handler stays free of
 * rule-specific branching: uniqueness violations report 409 CONFLICT,
 * every other rule defaults to 400 BAD_REQUEST.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /**
     * Shortcut for uniqueness violations, which must surface as HTTP 409.
     */
    public static BusinessException conflict(String message) {
        return new BusinessException(message, HttpStatus.CONFLICT);
    }
}
