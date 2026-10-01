package com.syncpoint.archive.dto;

import jakarta.validation.constraints.NotBlank;

public record StaffRegistrationRequest(
        @NotBlank String username,
        @NotBlank String password,   
        @NotBlank String firstName,
        @NotBlank String lastName,
        String staffNumber,
        String jobTitle,
        String department
) {}
