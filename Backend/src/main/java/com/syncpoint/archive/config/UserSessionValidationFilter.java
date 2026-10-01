package com.syncpoint.archive.config;

import com.syncpoint.archive.dao.AuthDao;
import com.syncpoint.archive.security.JsonErrors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;


public class UserSessionValidationFilter extends OncePerRequestFilter {

    private static final Set<String> KNOWN_ROLES = Set.of("PATIENT", "STAFF", "ADMIN");

    private final AuthDao authDao;

    public UserSessionValidationFilter(AuthDao authDao) {
        this.authDao = authDao;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        String currentRole = authDao.findUserRole(authentication.getName()).orElse(null);

        if (currentRole == null || !hasCurrentRole(authentication, currentRole)) {
            SecurityContextHolder.clearContext();
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            JsonErrors.write(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Your session is no longer valid. Please log in again.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean hasCurrentRole(Authentication authentication, String role) {
        if (!KNOWN_ROLES.contains(role)) {
            return false;
        }
        List<String> sessionRoles = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .toList();
        return sessionRoles.size() == 1 && sessionRoles.contains("ROLE_" + role);
    }
}
