package com.backend.finledger.common;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler translating exceptions into standard ApiError responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * HTTP 400 - Validation errors (field errors).
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new FieldError(err.getField(), err.getDefaultMessage()))
                .toList();

        String message = buildValidationMessage("Request has invalid fields", fieldErrors);
        log.warn("Validation failed on {}: {}", request.getRequestURI(), message);

        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    /**
     * HTTP 400 - Constraint violation errors.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = ex.getConstraintViolations().stream()
                .map(cv -> new FieldError(cv.getPropertyPath().toString(), cv.getMessage()))
                .toList();

        String message = buildValidationMessage("Constraint violation on request parameters", fieldErrors);
        log.warn("Constraint violation on {}: {}", request.getRequestURI(), message);

        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    /**
     * HTTP 400 - HandlerMethodValidationException (Spring method parameter validation).
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleHandlerMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        List<FieldError> fieldErrors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(err -> {
                fieldErrors.add(new FieldError(paramName != null ? paramName : "parameter", err.getDefaultMessage()));
            });
        });

        String message = buildValidationMessage("Request parameter validation failed", fieldErrors);
        log.warn("Parameter validation failed on {}: {}", request.getRequestURI(), message);

        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    /**
     * HTTP 400 - Path/Query parameter type mismatch.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String expectedType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "valid type";
        String detail = String.format("Expected %s but received '%s'", expectedType, ex.getValue());
        List<FieldError> fieldErrors = List.of(new FieldError(ex.getName(), detail));
        String message = buildValidationMessage("Invalid request parameter", fieldErrors);
        log.warn("Type mismatch on {}: {}", request.getRequestURI(), message);

        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_PARAMETER_TYPE",
                message,
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    /**
     * HTTP 400 - Malformed JSON / Illegal argument.
     */
    @ExceptionHandler({HttpMessageNotReadableException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception ex, HttpServletRequest request) {
        String detail = ex instanceof HttpMessageNotReadableException readable && readable.getMostSpecificCause() != null
                ? readable.getMostSpecificCause().getMessage()
                : ex.getMessage();
        log.warn("Bad request on {}: {}", request.getRequestURI(), detail);
        ApiError apiError = ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "BAD_REQUEST",
                detail != null && !detail.isBlank() ? detail : "Malformed request payload",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiError);
    }

    private String buildValidationMessage(String prefix, List<FieldError> fieldErrors) {
        if (fieldErrors == null || fieldErrors.isEmpty()) {
            return prefix;
        }
        String joined = fieldErrors.stream()
                .map(err -> err.field() + ": " + err.message())
                .collect(Collectors.joining(", "));
        return prefix + ": " + joined;
    }

    /**
     * HTTP 401 - Unauthenticated.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHENTICATED",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(apiError);
    }

    /**
     * HTTP 403 - Forbidden.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.FORBIDDEN.value(),
                ex.getCode() != null ? ex.getCode() : "FORBIDDEN",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.FORBIDDEN.value(),
                "FORBIDDEN",
                "Access is denied: " + ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(apiError);
    }

    /**
     * HTTP 404 - Not Found.
     */
    @ExceptionHandler({EntityNotFoundException.class, NoSuchElementException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(Exception ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "NOT_FOUND",
                ex.getMessage() != null ? ex.getMessage() : "Resource not found",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiError);
    }

    /**
     * HTTP 409 - Optimistic lock / Conflict / Data integrity violation.
     */
    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ApiError> handleOptimisticLock(Exception ex, HttpServletRequest request) {
        log.warn("Concurrent modification conflict on {}: {}", request.getRequestURI(), ex.getMessage());
        ApiError apiError = ApiError.of(
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                "Concurrent modification detected. The record was modified by another transaction. Please reload and try again.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity conflict on {}: {}", request.getRequestURI(), ex.getMessage());
        ApiError apiError = ApiError.of(
                HttpStatus.CONFLICT.value(),
                "CONFLICT",
                "Database constraint violation occurred.",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(apiError);
    }

    /**
     * HTTP 422 - Business rule violation.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        ApiError apiError = ApiError.of(
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getCode() != null ? ex.getCode() : "BUSINESS_RULE_VIOLATION",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(apiError);
    }

    /**
     * HTTP 500 - General internal server error fallback.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ApiError apiError = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred",
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiError);
    }
}
