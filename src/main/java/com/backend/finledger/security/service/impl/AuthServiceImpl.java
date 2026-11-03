package com.backend.finledger.security.service.impl;

import com.backend.finledger.common.BusinessException;
import com.backend.finledger.company.entity.Company;
import com.backend.finledger.company.mapper.CompanyMapper;
import com.backend.finledger.company.repository.CompanyRepository;
import com.backend.finledger.security.CurrentUser;
import com.backend.finledger.security.JwtService;
import com.backend.finledger.security.UserPrincipal;
import com.backend.finledger.security.dto.*;
import com.backend.finledger.security.entity.RefreshToken;
import com.backend.finledger.security.repository.RefreshTokenRepository;
import com.backend.finledger.security.service.AuthService;
import com.backend.finledger.user.entity.Permission;
import com.backend.finledger.user.entity.User;
import com.backend.finledger.user.entity.UserStatus;
import com.backend.finledger.user.mapper.UserMapper;
import com.backend.finledger.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final CompanyMapper companyMapper;

    public AuthServiceImpl(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            CompanyMapper companyMapper) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.companyMapper = companyMapper;
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.email().toLowerCase().trim());
        if (userOpt.isEmpty()) {
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        User user = userOpt.get();

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new BusinessException("ACCOUNT_LOCKED", "Account is locked due to 5 consecutive failed attempts");
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new BusinessException("ACCOUNT_DISABLED", "Account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = (user.getFailedAttempts() != null ? user.getFailedAttempts() : 0) + 1;
            user.setFailedAttempts((byte) attempts);
            if (attempts >= 5) {
                user.setStatus(UserStatus.LOCKED);
                userRepository.save(user);
                throw new BusinessException("ACCOUNT_LOCKED", "Account is locked due to 5 consecutive failed attempts");
            }
            userRepository.save(user);
            throw new BusinessException("INVALID_CREDENTIALS", "Invalid email or password");
        }

        // Reset failed attempts on success
        user.setFailedAttempts((byte) 0);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        Set<String> permissions = extractPermissions(user);
        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getCompanyId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                user.getStatus() == UserStatus.ACTIVE,
                permissions
        );

        String accessToken = jwtService.generateAccessToken(principal);
        String rawRefreshToken = jwtService.generateRefreshTokenValue();
        String hashedRefresh = JwtService.hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken(
                user.getId(),
                hashedRefresh,
                jwtService.calculateRefreshTokenExpiry()
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(accessToken, rawRefreshToken, userMapper.toDto(user), permissions);
    }

    @Override
    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        String tokenHash = JwtService.hashToken(request.refreshToken().trim());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException("INVALID_REFRESH_TOKEN", "Invalid refresh token"));

        if (Boolean.TRUE.equals(storedToken.getRevoked())) {
            throw new BusinessException("REFRESH_TOKEN_REVOKED", "Refresh token has been revoked");
        }
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("REFRESH_TOKEN_EXPIRED", "Refresh token has expired");
        }

        // Rotate token
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("ACCOUNT_INACTIVE", "User account is not active");
        }

        Set<String> permissions = extractPermissions(user);
        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getCompanyId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                true,
                permissions
        );

        String newAccessToken = jwtService.generateAccessToken(principal);
        String newRawRefreshToken = jwtService.generateRefreshTokenValue();
        String newHashedRefresh = JwtService.hashToken(newRawRefreshToken);

        RefreshToken newRefreshToken = new RefreshToken(
                user.getId(),
                newHashedRefresh,
                jwtService.calculateRefreshTokenExpiry()
        );
        refreshTokenRepository.save(newRefreshToken);

        return new TokenResponse(newAccessToken, newRawRefreshToken);
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            String tokenHash = JwtService.hashToken(refreshToken.trim());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(rt -> {
                rt.setRevoked(true);
                refreshTokenRepository.save(rt);
            });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse getMe() {
        Long userId = CurrentUser.getUserId();
        Long companyId = CurrentUser.getCompanyId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "Current user not found"));
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new BusinessException("COMPANY_NOT_FOUND", "Current company not found"));

        Set<String> permissions = extractPermissions(user);
        return new MeResponse(userMapper.toDto(user), companyMapper.toDto(company), permissions);
    }

    private Set<String> extractPermissions(User user) {
        if (user.getRoles() == null) {
            return Set.of();
        }
        Set<String> perms = new HashSet<>();
        user.getRoles().forEach(role -> {
            if (role.getPermissions() != null) {
                perms.addAll(role.getPermissions().stream()
                        .map(Permission::getCode)
                        .collect(Collectors.toSet()));
            }
        });
        return perms;
    }
}

