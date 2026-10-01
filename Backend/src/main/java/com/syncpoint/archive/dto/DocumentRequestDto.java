package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotNull;

public record DocumentRequestDto(
        @NotNull Long patientId,
        @NotNull Long requestedByUserId,
        @NotNull Integer documentTypeId,
        String requestReason
) {}
