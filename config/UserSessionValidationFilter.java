package com.hospitalarchive.hospitalarchivebackend.config;

import com.hospitalarchive.hospitalarchivebackend.model.User;
import com.hospitalarchive.hospitalarchivebackend.repository.UserRepository;

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

public class UserSessionValidationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public UserSessionValidationFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

       
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            filterChain.doFilter(request, response);
            return;
        }

        User user = userRepository.findByUsername(authentication.getName())
                .orElse(null);

        if (user == null || !hasCurrentRole(authentication, user.getRole())) {

            SecurityContextHolder.clearContext();

            HttpSession session = request.getSession(false);

            if (session != null) {
                session.invalidate();
            }

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }

    
    private boolean hasCurrentRole(
            Authentication authentication,
            String role) {

        if (role == null
                || (!role.equals("ADMIN")
                && !role.equals("ARCHIVE_STAFF")
                && !role.equals("READ_ONLY"))) {
            return false;
        }

        String expectedAuthority = "ROLE_" + role;

        List<String> sessionRoles = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .toList();

        return sessionRoles.size() == 1
                && sessionRoles.contains(expectedAuthority);
    }
}
