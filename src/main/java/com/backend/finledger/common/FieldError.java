package com.backend.finledger.common;

/**
 * Field-level validation error representation.
 */
public record FieldError(
    String field,
    String message
) {}

