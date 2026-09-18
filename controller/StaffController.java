
 package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.StaffDao;
import com.syncpoint.archive.dto.StaffRegistrationRequest;
import com.syncpoint.archive.util.PasswordUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    
    private static final Map<String, String> JOB_ROLE_DIGITS = Map.of(
            "doctor", "1", "nurse", "4", "receptionist", "5"
    );

    private final StaffDao staffDao;

    public StaffController(StaffDao staffDao) {
        this.staffDao = staffDao;
    }

   
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody StaffRegistrationRequest request) {
        String roleDigit = JOB_ROLE_DIGITS.get(request.jobTitle().trim().toLowerCase());
        if (roleDigit == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Unknown job title. Known titles: " + JOB_ROLE_DIGITS.keySet()));
        }

        String staffNumber = generateStaffNumber(roleDigit);
        String username = generateUsername(request.firstName(), staffNumber, request.jobTitle());
        String baseUsername = username;
        int attempt = 1;
        while (staffDao.usernameExists(username)) {
            attempt++;
            username = baseUsername + attempt;
        }

        String passwordHash = PasswordUtil.hashPassword(request.password());
        long staffId = staffDao.registerStaff(request, passwordHash, staffNumber, username);

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
