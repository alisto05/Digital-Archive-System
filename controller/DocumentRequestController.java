package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.DocumentRequestDao;
import com.syncpoint.archive.dto.DocumentRequestDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping
    public ResponseEntity<Map<String, Object>> requestDocument(@Valid @RequestBody DocumentRequestDto request) {
        long requestId = documentRequestDao.requestDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("requestId", requestId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Map<String, Object>>> getForPatient(@PathVariable long patientId) {
        return ResponseEntity.ok(documentRequestDao.getDocumentRequestsForPatient(patientId));
    }
}
