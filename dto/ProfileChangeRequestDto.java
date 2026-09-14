package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProfileChangeRequestDto(
        @NotNull Long patientId,
        @NotNull Long requestedByUserId,
        @NotBlank String fieldName,     
        String oldValue,
        @NotBlank String requestedValue,
        String reason
) {}
