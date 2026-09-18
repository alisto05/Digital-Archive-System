package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminRegistrationRequest(
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String firstName,
        @NotBlank String lastName
) {}
