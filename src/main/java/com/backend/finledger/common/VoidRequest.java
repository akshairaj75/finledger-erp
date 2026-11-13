package com.backend.finledger.common;

/**
 * Shared request payload for voiding/reversing financial documents per REST API spec Section 6.5.
 */
public record VoidRequest(String reason) {

    public static String resolveReason(VoidRequest body, String queryReason) {
        if (body != null && body.reason() != null && !body.reason().isBlank()) {
            return body.reason().trim();
        }
        return (queryReason != null && !queryReason.isBlank()) ? queryReason.trim() : null;
    }
}

