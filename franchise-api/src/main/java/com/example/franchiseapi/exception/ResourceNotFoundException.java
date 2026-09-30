package com.example.franchiseapi.exception;

/**
 * Raised when a referenced resource does not exist. Maps to HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Builds a uniform "not found" message, e.g. {@code Franchise with id 10 not found}.
     */
    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("%s with id %s not found".formatted(resource, id));
    }
}
