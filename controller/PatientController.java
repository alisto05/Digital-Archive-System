package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.PatientDao;
import com.syncpoint.archive.dto.PatientRegistrationRequest;
import com.syncpoint.archive.util.PasswordUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientDao patientDao;

    public PatientController(PatientDao patientDao) {
        this.patientDao = patientDao;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody PatientRegistrationRequest request) {
        String passwordHash = PasswordUtil.hashPassword(request.password());
        long patientId = patientDao.registerPatient(request, passwordHash);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("patientId", patientId));
    }

    @GetMapping("/{patientId}/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable long patientId) {
        return ResponseEntity.ok(patientDao.getPatientProfile(patientId));
    }
}
