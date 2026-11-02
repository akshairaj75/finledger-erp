package com.backend.finledger.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration accessTokenDuration;
    private final Duration refreshTokenDuration;
    private final SecureRandom secureRandom = new SecureRandom();

    public JwtService(
            @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}") String secret,
            @Value("${jwt.access-token-expiration-minutes:15}") long accessExpirationMinutes,
            @Value("${jwt.refresh-token-expiration-days:7}") long refreshExpirationDays) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            this.key = Keys.hmacShaKeyFor(padded);
        } else {
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
        this.accessTokenDuration = Duration.ofMinutes(accessExpirationMinutes);
        this.refreshTokenDuration = Duration.ofDays(refreshExpirationDays);
    }

    public String generateAccessToken(UserPrincipal principal) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenDuration);

        return Jwts.builder()
                .subject(principal.getEmail())
                .claim("userId", principal.getId())
                .claim("companyId", principal.getCompanyId())
                .claim("fullName", principal.getFullName())
                .claim("permissions", new ArrayList<>(principal.getPermissions()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public String generateRefreshTokenValue() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    public Instant calculateRefreshTokenExpiry() {
        return Instant.now().plus(refreshTokenDuration);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public Optional<Claims> parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public Optional<UserPrincipal> extractPrincipal(String token) {
        return parseToken(token).map(claims -> {
            Long userId = claims.get("userId", Long.class);
            if (userId == null && claims.get("userId") instanceof Number num) {
                userId = num.longValue();
            }
            Long companyId = claims.get("companyId", Long.class);
            if (companyId == null && claims.get("companyId") instanceof Number num) {
                companyId = num.longValue();
            }
            String email = claims.getSubject();
            String fullName = claims.get("fullName", String.class);
            List<String> permList = claims.get("permissions", List.class);
            Set<String> permissions = permList != null ? new HashSet<>(permList) : Collections.emptySet();

            return new UserPrincipal(userId, companyId, email, "", fullName, true, permissions);
        });
    }
}

