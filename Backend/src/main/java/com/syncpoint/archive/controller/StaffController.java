package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.StaffDao;
import com.syncpoint.archive.dto.StaffRegistrationRequest;
import com.syncpoint.archive.util.PasswordPolicy;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

  
    private static final Map<String, String> CANONICAL_TITLES = Map.of(
            "doctor", "Doctor", "nurse", "Nurse", "receptionist", "Receptionist");
    private static final Map<String, String> JOB_ROLE_DIGITS = Map.of(
            "doctor", "1", "nurse", "4", "receptionist", "5");

    private final StaffDao staffDao;
    private final PasswordEncoder passwordEncoder;

    public StaffController(StaffDao staffDao, PasswordEncoder passwordEncoder) {
        this.staffDao = staffDao;
        this.passwordEncoder = passwordEncoder;
    }

   
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody StaffRegistrationRequest request) {
        PasswordPolicy.validate(request.password());

        String titleKey = request.jobTitle().trim().toLowerCase();
        String canonicalTitle = CANONICAL_TITLES.get(titleKey);
        if (canonicalTitle == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown job title. Known titles: " + CANONICAL_TITLES.values());
        }

        String staffNumber = generateStaffNumber(JOB_ROLE_DIGITS.get(titleKey));
        String baseUsername = generateUsername(request.firstName().trim(), staffNumber, canonicalTitle);
        String username = baseUsername;
        int attempt = 1;
        while (staffDao.usernameExists(username)) {
            attempt++;
            username = baseUsername + attempt;
        }

        String email = request.email() == null || request.email().isBlank() ? null : request.email().trim();
        if (email != null && staffDao.emailExists(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A staff member with that email already exists.");
        }

        StaffRegistrationRequest canonical = new StaffRegistrationRequest(
                request.password(), request.firstName().trim(), request.lastName().trim(),
                canonicalTitle, request.courtesyTitle(), request.department(),
                email, request.specialization());

      
        long staffId = staffDao.registerStaff(
                canonical, passwordEncoder.encode(request.password()), staffNumber, username);

      
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "staffId", staffId,
                "username", username,
                "staffNumber", staffNumber
        ));
    }

    private String generateStaffNumber(String roleDigit) {
        String candidate;
        int seq = 1;
        do {
            candidate = String.format("S-%s%07d", roleDigit, seq);
            seq++;
        } while (staffDao.staffNumberExists(candidate));
        return candidate;
    }

    private String generateUsername(String firstName, String staffNumber, String jobTitle) {
        String namePart = firstName.length() > 6 ? firstName.substring(0, 6) : firstName;
        String digitsOnly = staffNumber.replaceAll("\\D", "");
        String numberPart = digitsOnly.length() >= 2 ? digitsOnly.substring(digitsOnly.length() - 2) : digitsOnly;
        String titlePart = jobTitle.substring(0, 1).toUpperCase();
        return "S-" + namePart + numberPart + "&" + titlePart;
    }
}
