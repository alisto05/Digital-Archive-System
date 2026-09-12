package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.StaffDao;
import com.syncpoint.archive.dto.StaffRegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffDao staffDao;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public StaffController(StaffDao staffDao) {
        this.staffDao = staffDao;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody StaffRegistrationRequest request) {
        String passwordHash = passwordEncoder.encode(request.password());
        long staffId = staffDao.registerStaff(request, passwordHash);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("staffId", staffId));
    }
}
