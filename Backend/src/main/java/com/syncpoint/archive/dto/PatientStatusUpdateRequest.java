package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** The reviewing staff member comes from the session, not from this body. */
public record PatientStatusUpdateRequest(
        @NotNull @Pattern(regexp = "APPROVED|REJECTED") String newStatus
) {}
