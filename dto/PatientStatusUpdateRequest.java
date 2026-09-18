package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PatientStatusUpdateRequest(
        @Pattern(regexp = "APPROVED|REJECTED") String newStatus,
        @NotNull Long reviewedByStaffId
) {}
