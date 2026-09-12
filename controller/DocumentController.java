package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.DocumentDao;
import com.syncpoint.archive.dto.DocumentRequestDto;
import com.syncpoint.archive.dto.DocumentReviewRequest;
import com.syncpoint.archive.dto.DocumentUploadRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentDao documentDao;

    public DocumentController(DocumentDao documentDao) {
        this.documentDao = documentDao;
    }

    /**
     * Registers a document record after the file itself has already been
     * saved to your storage backend (disk, S3, etc.) — this endpoint expects
     * `storageKey` to already point at that saved file.
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@Valid @RequestBody DocumentUploadRequest request) {
        long documentId = documentDao.uploadDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("documentId", documentId));
    }

    /** Approves or rejects a document. Triggers the DOCUMENT_APPROVE/DOCUMENT_REJECT audit rows. */
    @PutMapping("/{documentId}/review")
    public ResponseEntity<Void> review(@PathVariable long documentId,
                                        @Valid @RequestBody DocumentReviewRequest request) {
        documentDao.reviewDocument(documentId, request.newStatus(),
                request.reviewedByUserId(), request.rejectionReason());
        return ResponseEntity.noContent().build();
    }

    /** Staff requesting a specific document type from a patient. */
    @PostMapping("/request")
    public ResponseEntity<Map<String, Object>> requestDocument(@Valid @RequestBody DocumentRequestDto request) {
        long requestId = documentDao.requestDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("requestId", requestId));
    }
}
