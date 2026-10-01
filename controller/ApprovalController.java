package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.ApprovalDao;
import com.syncpoint.archive.dto.PatientStatusUpdateRequest;
import com.syncpoint.archive.security.Access;
import com.syncpoint.archive.security.AppUserDetails;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
                                                    @Valid @RequestBody PatientStatusUpdateRequest request,
                                                    @AuthenticationPrincipal AppUserDetails me) {
        if (me.staffId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only staff can review patients.");
        }

        switch (approvalDao.reviewPatient(patientId, request.newStatus(), me.staffId())) {
            case NOT_FOUND -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found.");
            case NOT_PENDING -> throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This patient has already been reviewed.");
            case REVIEWED -> { }
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/patients/{patientId}/recent-activity")
    public ResponseEntity<List<Map<String, Object>>> getRecentActivity(@PathVariable long patientId,
                                                                        @AuthenticationPrincipal AppUserDetails me) {
        Access.requirePatientAccess(me, patientId);
        return ResponseEntity.ok(approvalDao.getRecentActivityForPatient(patientId));
    }
}
