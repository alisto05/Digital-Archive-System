package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.DocumentRequestDao;
import com.syncpoint.archive.dto.DocumentRequestDto;
import com.syncpoint.archive.security.Access;
import com.syncpoint.archive.security.AppUserDetails;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/document-requests")
public class DocumentRequestController {

    private final DocumentRequestDao documentRequestDao;

    public DocumentRequestController(DocumentRequestDao documentRequestDao) {
        this.documentRequestDao = documentRequestDao;
    }

    /** Staff request a document from a patient. The requester is the logged-in staff member. */
    @PostMapping
    public ResponseEntity<Map<String, Object>> requestDocument(@Valid @RequestBody DocumentRequestDto request,
                                                               @AuthenticationPrincipal AppUserDetails me) {
        long requestId = documentRequestDao.requestDocument(
                request.patientId(), me.userId(), request.documentTypeId(), request.requestReason());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("requestId", requestId));
    }

    /** A patient sees the requests made to them; staff can see any patient's. */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Map<String, Object>>> getForPatient(@PathVariable long patientId,
                                                                   @AuthenticationPrincipal AppUserDetails me) {
        Access.requirePatientAccess(me, patientId);
        return ResponseEntity.ok(documentRequestDao.getDocumentRequestsForPatient(patientId));
    }
}
