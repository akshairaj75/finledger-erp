package com.backend.finledger;

import com.backend.finledger.common.ApiError;
import com.backend.finledger.common.BusinessException;
import com.backend.finledger.common.GlobalExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/test");
    }

    @Test
    @DisplayName("Should handle BusinessException with HTTP 422")
    void testBusinessException() {
        BusinessException ex = new BusinessException("RECEIPT_REQUIRED", "Receipt attachment is mandatory");
        ResponseEntity<ApiError> response = handler.handleBusinessException(ex, request);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(422, response.getBody().status());
        assertEquals("RECEIPT_REQUIRED", response.getBody().code());
        assertEquals("Receipt attachment is mandatory", response.getBody().message());
        assertEquals("/api/v1/test", response.getBody().path());
    }

    @Test
    @DisplayName("Should handle AuthenticationException with HTTP 401")
    void testAuthenticationException() {
        BadCredentialsException ex = new BadCredentialsException("Invalid credentials");
        ResponseEntity<ApiError> response = handler.handleAuthenticationException(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("UNAUTHENTICATED", response.getBody().code());
    }

    @Test
    @DisplayName("Should handle ForbiddenException with HTTP 403 and specific error code")
    void testForbiddenException() {
        com.backend.finledger.common.ForbiddenException ex =
                new com.backend.finledger.common.ForbiddenException("SELF_APPROVAL_NOT_ALLOWED", "Approver cannot approve their own claim");
        ResponseEntity<ApiError> response = handler.handleForbidden(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().status());
        assertEquals("SELF_APPROVAL_NOT_ALLOWED", response.getBody().code());
        assertEquals("Approver cannot approve their own claim", response.getBody().message());
    }

    @Test
    @DisplayName("Should handle AccessDeniedException with HTTP 403")
    void testAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");
        ResponseEntity<ApiError> response = handler.handleAccessDenied(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("FORBIDDEN", response.getBody().code());
    }

    @Test
    @DisplayName("Should handle EntityNotFoundException with HTTP 404")
    void testEntityNotFoundException() {
        EntityNotFoundException ex = new EntityNotFoundException("Customer not found");
        ResponseEntity<ApiError> response = handler.handleNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("NOT_FOUND", response.getBody().code());
    }

    @Test
    @DisplayName("Should handle OptimisticLockException with HTTP 409")
    void testOptimisticLockException() {
        OptimisticLockException ex = new OptimisticLockException("Row updated by concurrent transaction");
        ResponseEntity<ApiError> response = handler.handleOptimisticLock(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("CONFLICT", response.getBody().code());
    }
}

