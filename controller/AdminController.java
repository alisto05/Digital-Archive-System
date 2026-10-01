package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.AdminDao;
import com.syncpoint.archive.dto.AdminRegistrationRequest;
import com.syncpoint.archive.util.PasswordPolicy;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminDao adminDao;
    private final PasswordEncoder passwordEncoder;

    public AdminController(AdminDao adminDao, PasswordEncoder passwordEncoder) {
        this.adminDao = adminDao;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody AdminRegistrationRequest request) {
        PasswordPolicy.validate(request.password());

        if (adminDao.usernameExists(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken.");
        }
        long adminId = adminDao.registerAdmin(request, passwordEncoder.encode(request.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("adminId", adminId));
    }

    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getOverview() {
        return ResponseEntity.ok(adminDao.getOverviewStats());
    }

    @GetMapping("/staff")
    public ResponseEntity<List<Map<String, Object>>> getAllStaff() {
        return ResponseEntity.ok(adminDao.getAllStaff());
    }

    @GetMapping("/patients")
    public ResponseEntity<List<Map<String, Object>>> getAllPatients() {
        return ResponseEntity.ok(adminDao.getAllPatients());
    }
}
