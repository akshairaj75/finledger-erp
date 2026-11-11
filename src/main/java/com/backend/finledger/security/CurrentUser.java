package com.backend.finledger.security;

import com.backend.finledger.common.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Thread-safe context holder to access the current authenticated tenant and user.
 * Guarantees that every service and database query derives company_id from JWT claims,
 * never from the HTTP request body.
 */
public final class CurrentUser {

    private CurrentUser() {}

    public static Optional<UserPrincipal> getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static UserPrincipal getRequiredPrincipal() {
        return getPrincipal().orElseThrow(() ->
                new BusinessException("UNAUTHENTICATED", "No active authenticated user session found"));
    }

    public static Long getCompanyId() {
        return getRequiredPrincipal().getCompanyId();
    }

    public static Long getUserId() {
        return getRequiredPrincipal().getId();
    }

    public static String getEmail() {
        return getRequiredPrincipal().getEmail();
    }

    public static boolean hasAuthority(String authority) {
        return getPrincipal()
                .map(p -> p.getPermissions() != null && p.getPermissions().contains(authority))
                .orElse(false);
    }

    public static boolean hasAnyAuthority(String... authorities) {
        Optional<UserPrincipal> principalOpt = getPrincipal();
        if (principalOpt.isEmpty() || principalOpt.get().getPermissions() == null) {
            return false;
        }
        var granted = principalOpt.get().getPermissions();
        for (String a : authorities) {
            if (granted.contains(a)) {
                return true;
            }
        }
        return false;
    }
}

