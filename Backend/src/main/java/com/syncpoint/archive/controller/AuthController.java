package com.syncpoint.archive.controller;

import com.syncpoint.archive.config.PasswordConfig;
import com.syncpoint.archive.dao.AuthDao;
import com.syncpoint.archive.dto.LoginRequest;
import com.syncpoint.archive.security.AppUserDetails;
import com.syncpoint.archive.util.PasswordPolicy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthDao authDao;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

   
    private final String dummyHash;

    public AuthController(AuthDao authDao,
                          PasswordEncoder passwordEncoder,
                          SecurityContextRepository securityContextRepository,
                          SessionAuthenticationStrategy sessionAuthenticationStrategy) {
        this.authDao = authDao;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

  
    @GetMapping("/csrf")
    public Map<String, String> getCsrfToken(CsrfToken csrfToken) {
        return Map.of(
                "token", csrfToken.getToken(),
                "headerName", csrfToken.getHeaderName()
        );
    }

    @PostMapping("/login/patient")
    public ResponseEntity<Map<String, Object>> loginPatient(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest http,
                                                            HttpServletResponse response) {
        return login(AppUserDetails.PATIENT, authDao.getPatientLoginData(request.username()),
                request, http, response);
    }

    @PostMapping("/login/staff")
    public ResponseEntity<Map<String, Object>> loginStaff(@Valid @RequestBody LoginRequest request,
                                                          HttpServletRequest http,
                                                          HttpServletResponse response) {
        return login(AppUserDetails.STAFF, authDao.getStaffLoginData(request.username()),
                request, http, response);
    }

    @PostMapping("/login/admin")
    public ResponseEntity<Map<String, Object>> loginAdmin(@Valid @RequestBody LoginRequest request,
                                                          HttpServletRequest http,
                                                          HttpServletResponse response) {
        return login(AppUserDetails.ADMIN, authDao.getAdminLoginData(request.username()),
                request, http, response);
    }

    // ------------------------------------------------------------------

    private ResponseEntity<Map<String, Object>> login(String role,
                                                      Map<String, Object> record,
                                                      LoginRequest request,
                                                      HttpServletRequest http,
                                                      HttpServletResponse response) {

        boolean passwordUsable = PasswordPolicy.fitsBcrypt(request.password());
        Long userId = record != null ? ((Number) record.get("user_id")).longValue() : null;
        boolean credentialsOk;

        if (record == null) {
            if (passwordUsable) {
                passwordEncoder.matches(request.password(), dummyHash);
            }
            credentialsOk = false;
        } else {
            credentialsOk = passwordUsable
                    && checkPasswordAndMigrate(request.password(), (String) record.get("password_hash"), userId);
        }

        long loginId = authDao.recordLoginAttempt(userId, request.username(), credentialsOk, http.getRemoteAddr());

        if (!credentialsOk) {
            return error(HttpStatus.UNAUTHORIZED, "Invalid username or password.");
        }

      
        if (AppUserDetails.PATIENT.equals(role)) {
            String status = (String) record.get("status");
            if ("PENDING".equals(status)) {
                return error(HttpStatus.FORBIDDEN, "Your registration is still awaiting Staff Approval.");
            }
            if ("REJECTED".equals(status)) {
                return error(HttpStatus.FORBIDDEN, "Your registration was not approved. Contact SyncPoint support.");
            }
            if (!"APPROVED".equals(status)) {
                return error(HttpStatus.FORBIDDEN, "Your account is not active.");
            }
        }

        Long patientId = idOf(record, "patient_id");
        Long staffId = idOf(record, "staff_id");
        Long adminId = idOf(record, "admin_id");
        String username = (String) record.get("username");

        String displayName;
        if (AppUserDetails.PATIENT.equals(role)) {
            String preferred = (String) record.get("preferred_name");
            displayName = preferred != null && !preferred.isBlank() ? preferred : username;
        } else {
            displayName = record.get("first_name") + " " + record.get("last_name");
        }

        AppUserDetails user = new AppUserDetails(userId, username, role, patientId, staffId, adminId, loginId, displayName);
        establishSession(user, http, response);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", userId);
        body.put("role", role);
        if (patientId != null) body.put("patientId", patientId);
        if (staffId != null) body.put("staffId", staffId);
        if (adminId != null) body.put("adminId", adminId);
        body.put("displayName", displayName);
        return ResponseEntity.ok(body);
    }

    
    private boolean checkPasswordAndMigrate(String rawPassword, String storedHash, long userId) {
        boolean matches = passwordEncoder.matches(rawPassword, storedHash);
        if (matches && !PasswordConfig.isBCryptHash(storedHash)) {
            authDao.updatePasswordHash(userId, passwordEncoder.encode(rawPassword));
        }
        return matches;
    }

    private void establishSession(AppUserDetails user, HttpServletRequest request, HttpServletResponse response) {
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                user, null, user.getAuthorities());

        sessionAuthenticationStrategy.onAuthentication(authenticated, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authenticated);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private static Long idOf(Map<String, Object> record, String column) {
        Object value = record.get(column);
        return value == null ? null : ((Number) value).longValue();
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
