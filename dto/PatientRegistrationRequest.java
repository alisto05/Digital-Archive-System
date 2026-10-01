package com.syncpoint.archive.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PatientRegistrationRequest(
        @NotBlank String username,
        @NotBlank String password,         
        String title,
        @NotBlank String firstName,
        String middleName,
        @NotBlank String lastName,
        String preferredName,
        String idNumber,
        @NotNull LocalDate dateOfBirth,
        String country,
        String addressLine,
        String city,
        String postalCode,
        String province,
        String homePhone,
        String workPhone,
        String mobilePhone,
        String secondaryPhone,
        @Email String email,
        @Email String secondaryEmail,
        boolean hasMedicalAid,
        String provider,
        String membershipNumber
) {}
