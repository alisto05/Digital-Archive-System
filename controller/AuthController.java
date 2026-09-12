package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.AuthDao;
import com.syncpoint.archive.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthDao authDao;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(AuthDao authDao) {
        this.authDao = authDao;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request,
                                                       HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        Map<String, Object> user = authDao.findUserForLogin(request.username());

        boolean success = user != null
                && passwordEncoder.matches(request.password(), (String) user.get("password_hash"));

        Long userId = user != null ? ((Number) user.get("user_id")).longValue() : null;

        // sp_record_login_attempt fires the audit-log trigger and, on success,
        // updates users.last_login_at — regardless of outcome, log the attempt.
        long loginId = authDao.recordLoginAttempt(userId, request.username(), success, ip);

        if (!success) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password", "loginId", loginId));
        }

        return ResponseEntity.ok(Map.of(
                "loginId", loginId,
                "userId", userId,
                "role", user.get("role")
                // In a real deployment, issue a session token / JWT here instead of
                // just returning the raw user id.
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam long loginId) {
        authDao.recordLogout(loginId);
        return ResponseEntity.noContent().build();
    }
}
