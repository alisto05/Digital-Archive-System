package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.AdminDao;
import com.syncpoint.archive.dto.AdminRegistrationRequest;
import com.syncpoint.archive.util.PasswordUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminDao adminDao;

    public AdminController(AdminDao adminDao) {
        this.adminDao = adminDao;
    }

    
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody AdminRegistrationRequest request) {
        if (adminDao.usernameExists(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "That username is already taken."));
        }
        String passwordHash = PasswordUtil.hashPassword(request.password());
        long adminId = adminDao.registerAdmin(request, passwordHash);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("adminId", adminId));
    }
}
