package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentUploadRequest(
        @NotNull Long patientId,
        @NotNull Long uploadedByUserId,
        @NotNull Integer documentTypeId,
        @NotBlank String originalFilename,
        @NotBlank String storageKey,      
        String mimeType,
        Long fileSizeBytes,
        String checksumSha256
) {}
