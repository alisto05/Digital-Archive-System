package com.syncpoint.archive.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * The logged-in user, stored in the session. Controllers read WHO is acting from
 * here (never from the request body): userId for "uploaded by" / "reviewed by",
 * patientId for ownership checks, staffId for patient approvals, loginId so the
 * logout can be recorded against the right login row.
 */
public record AppUserDetails(
        long userId,
        String username,
        String role,
        Long patientId,
        Long staffId,
        Long adminId,
        long loginId,
        String displayName
) implements UserDetails {

    public static final String PATIENT = "PATIENT";
    public static final String STAFF = "STAFF";
    public static final String ADMIN = "ADMIN";

    public boolean isPatient() {
        return PATIENT.equals(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    // Credentials are never kept in the session
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
