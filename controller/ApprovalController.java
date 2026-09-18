package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.ApprovalDao;
import com.syncpoint.archive.dto.PatientStatusUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final ApprovalDao approvalDao;

    public ApprovalController(ApprovalDao approvalDao) {
        this.approvalDao = approvalDao;
    }

    @GetMapping("/pending-patients")
    public ResponseEntity<List<Map<String, Object>>> getPendingPatients() {
        return ResponseEntity.ok(approvalDao.getPendingPatients());
    }

    @PutMapping("/patients/{patientId}/status")
    public ResponseEntity<Void> updatePatientStatus(@PathVariable long patientId,
                                                      @Valid @RequestBody PatientStatusUpdateRequest request) {
        approvalDao.updatePatientStatus(patientId, request.newStatus(), request.reviewedByStaffId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/patients/{patientId}/recent-activity")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity(@PathVariable long patientId) {
        return ResponseEntity.ok(approvalDao.getRecentActivityForPatient(patientId));
    }
}
