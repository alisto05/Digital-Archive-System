package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.PatientDao;
import com.syncpoint.archive.dao.SearchAndStatsDao;
import com.syncpoint.archive.dto.PatientRegistrationRequest;
import com.syncpoint.archive.security.Access;
import com.syncpoint.archive.security.AppUserDetails;
import com.syncpoint.archive.util.PasswordPolicy;
import com.syncpoint.archive.util.SearchTerms;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientDao patientDao;
    private final PasswordEncoder passwordEncoder;

    private final SearchAndStatsDao searchAndStatsDao;

    public PatientController(PatientDao patientDao, PasswordEncoder passwordEncoder,
                             SearchAndStatsDao searchAndStatsDao) {
        this.patientDao = patientDao;
        this.passwordEncoder = passwordEncoder;
        this.searchAndStatsDao = searchAndStatsDao;
    }

  
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> search(@RequestParam("search") String search,
                                                            @AuthenticationPrincipal AppUserDetails me) {
        String term = SearchTerms.validate(search);
        List<Map<String, Object>> results = patientDao.searchApprovedPatients(term);
        searchAndStatsDao.recordSearch(me.userId(), term, "PATIENT_LOOKUP", results.size());
        return ResponseEntity.ok(results);
    }

    
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody PatientRegistrationRequest request) {
        PasswordPolicy.validate(request.password());

        if (patientDao.usernameExists(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already taken.");
        }
        if (patientDao.idNumberExists(request.idNumber())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A patient with that ID number is already registered.");
        }

        long patientId = patientDao.registerPatient(request, passwordEncoder.encode(request.password()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("patientId", patientId, "status", "PENDING"));
    }

    @GetMapping("/{patientId}/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@PathVariable long patientId,
                                                          @AuthenticationPrincipal AppUserDetails me) {
        Access.requirePatientAccess(me, patientId);

        return patientDao.getPatientProfile(patientId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found."));
    }
}
