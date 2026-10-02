package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The reviewer comes from the session, not from this body. A reason is required when rejecting. */
public record DocumentReviewRequest(
        @NotNull @Pattern(regexp = "APPROVED|REJECTED") String newStatus,
        @Size(max = 1000) String rejectionReason
) {}
