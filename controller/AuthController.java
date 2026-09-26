package com.syncpoint.archive.controller;

import com.syncpoint.archive.config.PasswordConfig;
import com.syncpoint.archive.dao.AuthDao;
import com.syncpoint.archive.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthDao authDao;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

    public AuthController(AuthDao authDao, PasswordEncoder passwordEncoder,
                           UserDetailsService userDetailsService,
                           SecurityContextRepository securityContextRepository,
                           SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.authDao = authDao;
        this.passwordEncoder = passwordEncoder;
        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
    }

    
    private boolean checkPasswordAndMigrate(String rawPassword, String storedHash, long userId) {
        boolean matches = passwordEncoder.matches(rawPassword, storedHash);
        if (matches && !PasswordConfig.isBCryptHash(storedHash)) {
            authDao.updatePasswordHash(userId, passwordEncoder.encode(rawPassword));
        }
        return matches;
    }

  
    private void establishSession(String username, HttpServletRequest request, HttpServletResponse response) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                userDetails, null, userDetails.getAuthorities());

        sessionAuthenticationStrategy.onAuthentication(authenticated, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticated);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    @PostMapping("/login/patient")
    public ResponseEntity<Map<String, Object>> loginPatient(@Valid @RequestBody LoginRequest request,
                                                              HttpServletRequest httpRequest,
                                                              HttpServletResponse httpResponse) {
        String ip = httpRequest.getRemoteAddr();
        Map<String, Object> record = authDao.getPatientLoginData(request.username());

        Long userId = record != null ? ((Number) record.get("user_id")).longValue() : null;
        boolean credentialsOk = record != null
                && checkPasswordAndMigrate(request.password(), (String) record.get("password_hash"), userId);

        long loginId = authDao.recordLoginAttempt(userId, request.username(), credentialsOk, ip);

        if (record == null || !credentialsOk) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login Failed", "loginId", loginId));
        }

        String status = (String) record.get("status");
        if ("PENDING".equals(status)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Your registration is still awaiting Staff Approval.", "loginId", loginId));
        }
        if ("REJECTED".equals(status)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Your Registration was not approved. Contact SyncPoint support.", "loginId", loginId));
        }

        establishSession(request.username(), httpRequest, httpResponse);

        String preferredName = (String) record.get("preferred_name");
        return ResponseEntity.ok(Map.of(
                "loginId", loginId,
                "userId", userId,
                "patientId", record.get("patient_id"),
                "displayName", preferredName != null ? preferredName : request.username()
        ));
    }

    @PostMapping("/login/staff")
    public ResponseEntity<Map<String, Object>> loginStaff(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest httpRequest,
                                                            HttpServletResponse httpResponse) {
        String ip = httpRequest.getRemoteAddr();
        Map<String, Object> record = authDao.getStaffLoginData(request.username());

        Long userId = record != null ? ((Number) record.get("user_id")).longValue() : null;
        boolean credentialsOk = record != null
                && checkPasswordAndMigrate(request.password(), (String) record.get("password_hash"), userId);

        long loginId = authDao.recordLoginAttempt(userId, request.username(), credentialsOk, ip);

        if (record == null || !credentialsOk) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login Failed", "loginId", loginId));
        }

        establishSession(request.username(), httpRequest, httpResponse);

        return ResponseEntity.ok(Map.of(
                "loginId", loginId,
                "userId", userId,
                "staffId", record.get("staff_id"),
                "displayName", record.get("first_name") + " " + record.get("last_name")
        ));
    }

    @PostMapping("/login/admin")
    public ResponseEntity<Map<String, Object>> loginAdmin(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest httpRequest,
                                                            HttpServletResponse httpResponse) {
        String ip = httpRequest.getRemoteAddr();
        Map<String, Object> record = authDao.getAdminLoginData(request.username());

        Long userId = record != null ? ((Number) record.get("user_id")).longValue() : null;
        boolean credentialsOk = record != null
                && checkPasswordAndMigrate(request.password(), (String) record.get("password_hash"), userId);

        long loginId = authDao.recordLoginAttempt(userId, request.username(), credentialsOk, ip);

        if (record == null || !credentialsOk) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login Failed", "loginId", loginId));
        }

        establishSession(request.username(), httpRequest, httpResponse);

        return ResponseEntity.ok(Map.of(
                "loginId", loginId,
                "userId", userId,
                "adminId", record.get("admin_id"),
                "displayName", record.get("first_name") + " " + record.get("last_name")
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam long loginId, HttpServletRequest request) {
        authDao.recordLogout(loginId);
        SecurityContextHolder.clearContext();
        request.getSession().invalidate();
        return ResponseEntity.noContent().build();
    }
}

