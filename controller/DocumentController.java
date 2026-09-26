package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.DocumentDao;
import com.syncpoint.archive.dao.SearchAndStatsDao;
import com.syncpoint.archive.dto.DocumentReviewRequest;
import com.syncpoint.archive.dto.DocumentUploadRequest;
import com.syncpoint.archive.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentDao documentDao;
    private final SearchAndStatsDao searchAndStatsDao;
    private final FileStorageService fileStorageService;

    public DocumentController(DocumentDao documentDao, SearchAndStatsDao searchAndStatsDao,
                               FileStorageService fileStorageService) {
        this.documentDao = documentDao;
        this.searchAndStatsDao = searchAndStatsDao;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/types")
    public ResponseEntity<List<Map<String, Object>>> getDocumentTypes() {
        return ResponseEntity.ok(documentDao.getDocumentTypes());
    }

   
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long patientId,
            @RequestParam Long uploadedByUserId,
            @RequestParam Integer documentTypeId) {
        try {
            FileStorageService.StoredFile stored = fileStorageService.storePdf(file);

            DocumentUploadRequest request = new DocumentUploadRequest(
                    patientId, uploadedByUserId, documentTypeId,
                    file.getOriginalFilename(), stored.storageKey(), file.getContentType(),
                    stored.fileSizeBytes(), stored.checksumSha256()
            );
            long documentId = documentDao.uploadDocument(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("documentId", documentId));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not save the uploaded file."));
        }
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Map<String, Object>>> getForPatient(
            @PathVariable long patientId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long searchingUserId) {
        List<Map<String, Object>> results = documentDao.getDocumentsForPatient(patientId, search);
        if (search != null && !search.isBlank() && searchingUserId != null) {
            searchAndStatsDao.recordSearch(searchingUserId, search, "PATIENT", results.size());
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/staff-search")
    public ResponseEntity<List<Map<String, Object>>> searchForStaff(
            @RequestParam(required = false) String search,
            @RequestParam Long searchingUserId) {
        List<Map<String, Object>> results = documentDao.searchDocumentsForStaff(search);
        if (search != null && !search.isBlank()) {
            searchAndStatsDao.recordSearch(searchingUserId, search, "STAFF", results.size());
        }
        return ResponseEntity.ok(results);
    }

    @GetMapping("/pending-review")
    public ResponseEntity<List<Map<String, Object>>> getPendingReview() {
        return ResponseEntity.ok(documentDao.getPendingDocumentsForStaff());
    }

    @PutMapping("/{documentId}/review")
    public ResponseEntity<Void> review(@PathVariable long documentId,
                                        @Valid @RequestBody DocumentReviewRequest request) {
        documentDao.reviewDocument(documentId, request.newStatus(),
                request.reviewedByUserId(), request.rejectionReason());
        return ResponseEntity.noContent().build();
    }
}


