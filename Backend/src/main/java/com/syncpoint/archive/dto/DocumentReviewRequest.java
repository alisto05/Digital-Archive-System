package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DocumentReviewRequest(
        @NotNull Long reviewedByUserId,
        @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String newStatus,
        String rejectionReason
) {}
