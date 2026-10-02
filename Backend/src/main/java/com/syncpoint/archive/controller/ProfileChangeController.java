package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.PatientDao;
import com.syncpoint.archive.dao.ProfileChangeDao;
import com.syncpoint.archive.dto.ProfileChangeRequestDto;
import com.syncpoint.archive.dto.ProfileChangeResolveRequest;
import com.syncpoint.archive.security.Access;
import com.syncpoint.archive.security.AppUserDetails;
import com.syncpoint.archive.util.ProfileFields;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Patients ask for a detail on their profile to change; staff approve or reject.
 * Approval applies the change through sp_resolve_change_request.
 */
@RestController
@RequestMapping("/api/profile-change-requests")
public class ProfileChangeController {

    private final ProfileChangeDao profileChangeDao;
    private final PatientDao patientDao;

    public ProfileChangeController(ProfileChangeDao profileChangeDao, PatientDao patientDao) {
        this.profileChangeDao = profileChangeDao;
        this.patientDao = patientDao;
    }

    /** A patient files a request for themselves. The old value is read from the database, not the client. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> submit(@Valid @RequestBody ProfileChangeRequestDto request,
                                                      @AuthenticationPrincipal AppUserDetails me) {
        if (me.patientId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only patients can request profile changes.");
        }

        String field = request.fieldName().trim();
        String newValue = request.requestedValue().trim();
        ProfileFields.validate(field, newValue);

        Map<String, Object> profile = patientDao.getPatientProfile(me.patientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found."));
        Object current = profile.get(field);
        String oldValue = current == null ? null : current.toString();

        if (newValue.equals(oldValue)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The new value is the same as the current value.");
        }

        String reason = request.reason() == null || request.reason().isBlank() ? null : request.reason().trim();
        long id = profileChangeDao.submit(me.patientId(), me.userId(), field, oldValue, newValue, reason);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("changeRequestId", id, "status", "PENDING"));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Map<String, Object>>> getForPatient(@PathVariable long patientId,
                                                                   @AuthenticationPrincipal AppUserDetails me) {
        Access.requirePatientAccess(me, patientId);
        return ResponseEntity.ok(profileChangeDao.getForPatient(patientId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<Map<String, Object>>> getPending() {
        return ResponseEntity.ok(profileChangeDao.getPending());
    }

    /** The reviewer is the logged-in staff member; only PENDING requests can be resolved. */
    @PutMapping("/{changeRequestId}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable long changeRequestId,
                                        @Valid @RequestBody ProfileChangeResolveRequest request,
                                        @AuthenticationPrincipal AppUserDetails me) {
        switch (profileChangeDao.resolveIfPending(changeRequestId, request.approve(), me.userId())) {
            case NOT_FOUND -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Change request not found.");
            case NOT_PENDING -> throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This request has already been resolved.");
            case REVIEWED -> { }
        }
        return ResponseEntity.noContent().build();
    }
}
