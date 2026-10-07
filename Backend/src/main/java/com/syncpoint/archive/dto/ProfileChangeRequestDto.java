package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A patient asking for one detail on their profile to change. Which patient (the caller) and the
 * current value (read from the database) are decided by the server.
 */
public record ProfileChangeRequestDto(
        @NotBlank @Size(max = 100) String fieldName,
        @NotBlank @Size(max = 1000) String requestedValue,
        @Size(max = 2000) String reason
) {}
