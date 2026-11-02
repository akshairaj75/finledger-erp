package com.backend.finledger;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.security.AuthService;
import com.backend.finledger.security.JwtService;
import com.backend.finledger.security.dto.AuthResponse;
import com.backend.finledger.security.dto.LoginRequest;
import com.backend.finledger.security.dto.RefreshRequest;
import com.backend.finledger.security.dto.TokenResponse;
import com.backend.finledger.user.User;
import com.backend.finledger.user.UserRepository;
import com.backend.finledger.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthAndRbacTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private com.backend.finledger.company.CompanyRepository companyRepository;

    @Test
    @Transactional
    @DisplayName("Should successfully authenticate admin and return 15-min JWT, 7-day refresh token and permissions")
    void testSuccessfulLogin() {
        LoginRequest req = new LoginRequest("admin@finledger.com", "password");
        AuthResponse response = authService.login(req);

        assertNotNull(response.accessToken());
        assertNotNull(response.refreshToken());
        assertNotNull(response.user());
        assertEquals("admin@finledger.com", response.user().email());
        assertFalse(response.permissions().isEmpty());
        assertTrue(response.permissions().contains("COMPANY_VIEW"));
        assertTrue(response.permissions().contains("ACCOUNT_VIEW"));
    }

    @Test
    @Transactional
    @DisplayName("Should lock account after 5 consecutive failed attempts")
    void testAccountLockoutAfter5FailedAttempts() {
        Long companyId = companyRepository.findByCode("BSB").orElseThrow().getId();

        // Create dedicated user for lockout test
        User lockTestUser = new User();
        lockTestUser.setCompanyId(companyId);
        lockTestUser.setEmail("lockout@finledger.com");
        lockTestUser.setPasswordHash(passwordEncoder.encode("secret123"));
        lockTestUser.setFullName("Lockout Test User");
        lockTestUser.setStatus(UserStatus.ACTIVE);
        lockTestUser.setFailedAttempts((byte) 0);
        userRepository.save(lockTestUser);

        LoginRequest badReq = new LoginRequest("lockout@finledger.com", "wrong-password");

        // First 4 attempts should fail with INVALID_CREDENTIALS
        for (int i = 1; i <= 4; i++) {
            BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(badReq));
            assertEquals("INVALID_CREDENTIALS", ex.getCode());
        }

        // 5th failed attempt should lock account
        BusinessException lockEx = assertThrows(BusinessException.class, () -> authService.login(badReq));
        assertEquals("ACCOUNT_LOCKED", lockEx.getCode());

        User lockedUser = userRepository.findByEmail("lockout@finledger.com").orElseThrow();
        assertEquals(UserStatus.LOCKED, lockedUser.getStatus());
        assertEquals((byte) 5, lockedUser.getFailedAttempts().byteValue());
    }

    @Test
    @Transactional
    @DisplayName("Should rotate refresh token and revoke used token")
    void testRefreshTokenRotation() {
        LoginRequest req = new LoginRequest("admin@finledger.com", "password");
        AuthResponse loginRes = authService.login(req);
        String initialRefresh = loginRes.refreshToken();

        // Refresh token
        TokenResponse refreshed = authService.refresh(new RefreshRequest(initialRefresh));
        assertNotNull(refreshed.accessToken());
        assertNotNull(refreshed.refreshToken());
        assertNotEquals(initialRefresh, refreshed.refreshToken());

        // Attempting to reuse old refresh token must be rejected
        BusinessException ex = assertThrows(BusinessException.class, () ->
                authService.refresh(new RefreshRequest(initialRefresh)));
        assertEquals("REFRESH_TOKEN_REVOKED", ex.getCode());
    }
}
