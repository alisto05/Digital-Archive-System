package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.AuthDao;
import com.syncpoint.archive.dto.LoginRequest;
import com.syncpoint.archive.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthDao authDao;

    public AuthController(AuthDao authDao) {
        this.authDao = authDao;
    }

    @PostMapping("/login/patient")
    public ResponseEntity<Map<String, Object>> loginPatient(@Valid @RequestBody LoginRequest request,
                                                              HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        Map<String, Object> record = authDao.getPatientLoginData(request.username());

        boolean credentialsOk = record != null
                && PasswordUtil.verifyPassword(request.password(), (String) record.get("password_hash"));

        Long userId = record != null ? ((Number) record.get("user_id")).longValue() : null;
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
                                                            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        Map<String, Object> record = authDao.getStaffLoginData(request.username());

        boolean credentialsOk = record != null
                && PasswordUtil.verifyPassword(request.password(), (String) record.get("password_hash"));

        Long userId = record != null ? ((Number) record.get("user_id")).longValue() : null;
        long loginId = authDao.recordLoginAttempt(userId, request.username(), credentialsOk, ip);

        if (record == null || !credentialsOk) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login Failed", "loginId", loginId));
        }

        return ResponseEntity.ok(Map.of(
                "loginId", loginId,
                "userId", userId,
                "staffId", record.get("staff_id"),
                "displayName", record.get("first_name") + " " + record.get("last_name")
        ));
    }

    @PostMapping("/login/admin")
    public ResponseEntity<Map<String, Object>> loginAdmin(@Valid @RequestBody LoginRequest request,
                                                            HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        Map<String, Object> record = authDao.getAdminLoginData(request.username());

        boolean credentialsOk = record != null
                && PasswordUtil.verifyPassword(request.password(), (String) record.get("password_hash"));

        Long userId = record != null ? ((Number) record.get("user_id")).longValue() : null;
        long loginId = authDao.recordLoginAttempt(userId, request.username(), credentialsOk, ip);

        if (record == null || !credentialsOk) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Login Failed", "loginId", loginId));
        }

        return ResponseEntity.ok(Map.of(
                "loginId", loginId,
                "userId", userId,
                "adminId", record.get("admin_id"),
                "displayName", record.get("first_name") + " " + record.get("last_name")
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestParam long loginId) {
        authDao.recordLogout(loginId);
        return ResponseEntity.noContent().build();
    }
}
