package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotNull;

public record ProfileChangeResolutionDto(
        @NotNull Boolean approve,
        @NotNull Long reviewedByUserId
) {}
