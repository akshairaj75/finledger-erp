package com.backend.finledger.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Authenticated user principal holding multi-tenant identity and permissions.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final Long companyId;
    private final String email;
    private final String password;
    private final String fullName;
    private final boolean active;
    private final Set<String> permissions;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, Long companyId, String email, String password, String fullName, boolean active, Set<String> permissions) {
        this.id = id;
        this.companyId = companyId;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.active = active;
        this.permissions = permissions != null ? permissions : Collections.emptySet();
        this.authorities = this.permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Long getId() {
        return id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}

