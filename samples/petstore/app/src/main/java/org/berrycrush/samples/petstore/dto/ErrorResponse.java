package org.berrycrush.samples.petstore.dto;

import java.util.List;

/**
 * Standard error response format.
 */
public record ErrorResponse(
    int code,
    String message,
    List<String> details
) {
    /**
     * Creates an error response without details.
     */
    public static ErrorResponse of(int code, String message) {
        return new ErrorResponse(code, message, null);
    }

    /**
     * Creates an error response with details.
     */
    public static ErrorResponse of(int code, String message, List<String> details) {
        return new ErrorResponse(code, message, details);
    }
}
