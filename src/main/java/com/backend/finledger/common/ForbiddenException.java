package com.backend.finledger.common;

/**
 * Custom runtime exception for authorization and policy forbidden actions (maps to HTTP 403).
 */
public class ForbiddenException extends RuntimeException {

    private final String code;

    public ForbiddenException(String message) {
        super(message);
        this.code = "FORBIDDEN";
    }

    public ForbiddenException(String code, String message) {
        super(message);
        this.code = code;
    }

    public ForbiddenException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

