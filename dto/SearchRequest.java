package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SearchRequest(
        @NotNull Long userId,
        @NotBlank String searchTerm,
        String searchScope,      
        int resultsCount
) {}
