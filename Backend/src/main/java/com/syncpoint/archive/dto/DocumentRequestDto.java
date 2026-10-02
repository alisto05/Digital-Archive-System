package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Staff asking a patient for a document. The requesting staff member comes from the session. */
public record DocumentRequestDto(
        @NotNull Long patientId,
        @NotNull Integer documentTypeId,
        @Size(max = 2000) String requestReason
) {}
