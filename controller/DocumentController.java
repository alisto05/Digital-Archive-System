package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.DocumentDao;
import com.syncpoint.archive.dao.SearchAndStatsDao;
import com.syncpoint.archive.dto.DocumentReviewRequest;
import com.syncpoint.archive.security.Access;
import com.syncpoint.archive.security.AppUserDetails;
import com.syncpoint.archive.service.DocumentUploadService;
import com.syncpoint.archive.service.FileStorageService;

import jakarta.validation.Valid;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentDao documentDao;
    private final SearchAndStatsDao searchAndStatsDao;
    private final DocumentUploadService documentUploadService;
    private final FileStorageService fileStorageService;

    public DocumentController(DocumentDao documentDao,
                              SearchAndStatsDao searchAndStatsDao,
                              DocumentUploadService documentUploadService,
                              FileStorageService fileStorageService) {
        this.documentDao = documentDao;
        this.searchAndStatsDao = searchAndStatsDao;
        this.documentUploadService = documentUploadService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/types")
    public ResponseEntity<List<Map<String, Object>>> getDocumentTypes() {
        return ResponseEntity.ok(documentDao.getDocumentTypes());
    }

  
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "patientId", required = false) Long patientId,
            @RequestParam("documentTypeId") Integer documentTypeId,
            @AuthenticationPrincipal AppUserDetails me) throws IOException {

        long targetPatientId = Access.patientIdFor(me, patientId);
        long documentId = documentUploadService.upload(file, targetPatientId, documentTypeId, me.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("documentId", documentId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Map<String, Object>>> getForPatient(
            @PathVariable long patientId,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal AppUserDetails me) {

        Access.requirePatientAccess(me, patientId);

        List<Map<String, Object>> results = documentDao.getDocumentsForPatient(patientId, search);
        if (search != null && !search.isBlank()) {
            searchAndStatsDao.recordSearch(me.userId(), search, "PATIENT", results.size());
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/staff-search")
    public ResponseEntity<List<Map<String, Object>>> searchForStaff(
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal AppUserDetails me) {

        List<Map<String, Object>> results = documentDao.searchDocumentsForStaff(search);
        if (search != null && !search.isBlank()) {
            searchAndStatsDao.recordSearch(me.userId(), search, "STAFF", results.size());
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/pending-review")
    public ResponseEntity<List<Map<String, Object>>> getPendingReview() {
        return ResponseEntity.ok(documentDao.getPendingDocumentsForStaff());
    }

   
    @PutMapping("/{documentId}/review")
    public ResponseEntity<Void> review(@PathVariable long documentId,
                                       @Valid @RequestBody DocumentReviewRequest request,
                                       @AuthenticationPrincipal AppUserDetails me) {

        boolean rejecting = "REJECTED".equals(request.newStatus());
        String reason = request.rejectionReason() == null ? null : request.rejectionReason().trim();

        if (rejecting && (reason == null || reason.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A rejection reason is required.");
        }

        switch (documentDao.reviewIfPending(documentId, request.newStatus(), me.userId(), rejecting ? reason : null)) {
            case NOT_FOUND -> throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found.");
            case NOT_PENDING -> throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This document has already been reviewed.");
            case REVIEWED -> { }
        }
        return ResponseEntity.noContent().build();
    }

    
    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> download(@PathVariable long documentId,
                                             @AuthenticationPrincipal AppUserDetails me) throws IOException {

        Map<String, Object> document = documentDao.findDocument(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found."));

        Access.requirePatientAccess(me, ((Number) document.get("patient_id")).longValue());

        Resource file;
        try {
            file = fileStorageService.loadAsResource((String) document.get("storage_key"));
        } catch (FileNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The file for this document is not available.");
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(file.contentLength())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename((String) document.get("original_filename"), StandardCharsets.UTF_8)
                        .build().toString())
                .body(file);
    }
}
