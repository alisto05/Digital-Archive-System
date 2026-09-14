package com.syncpoint.archive.controller;

import com.syncpoint.archive.dao.ProfileChangeDao;
import com.syncpoint.archive.dto.ProfileChangeRequestDto;
import com.syncpoint.archive.dto.ProfileChangeResolutionDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile-changes")
public class ProfileChangeController {

    private final ProfileChangeDao profileChangeDao;

    public ProfileChangeController(ProfileChangeDao profileChangeDao) {
        this.profileChangeDao = profileChangeDao;
    }

    
    @PostMapping
    public ResponseEntity<Map<String, Object>> submit(@Valid @RequestBody ProfileChangeRequestDto request) {
        long changeRequestId = profileChangeDao.submitChangeRequest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("changeRequestId", changeRequestId));
    }

 
    @PutMapping("/{changeRequestId}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable long changeRequestId,
                                         @Valid @RequestBody ProfileChangeResolutionDto request) {
        profileChangeDao.resolveChangeRequest(changeRequestId, request.approve(), request.reviewedByUserId());
        return ResponseEntity.noContent().build();
    }
}
