package com.syncpoint.archive.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** username and staff number are generated on the server; the admin supplies the initial password. */
public record StaffRegistrationRequest(
        @NotBlank String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Size(max = 100) String jobTitle,
        @Size(max = 10) String courtesyTitle,
        @Size(max = 150) String department,
        @Email @Size(max = 255) String email,
        @Size(max = 150) String specialization
) {}
