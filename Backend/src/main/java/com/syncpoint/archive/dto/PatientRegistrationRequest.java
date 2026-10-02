package com.syncpoint.archive.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Field lengths match the column sizes in the schema so bad input is a 400, not a database error. */
public record PatientRegistrationRequest(
        @NotBlank @Size(max = 50) String username,
        @NotBlank String password,
        @Size(max = 10) String title,
        @NotBlank @Size(max = 100) String firstName,
        @Size(max = 100) String middleName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 100) String preferredName,
        @NotBlank @Size(max = 20) String idNumber,
        @NotNull @Past LocalDate dateOfBirth,
        @Size(max = 100) String country,
        @Size(max = 255) String addressLine,
        @Size(max = 100) String city,
        @Size(max = 20) String postalCode,
        @Size(max = 100) String province,
        @Size(max = 30) String homePhone,
        @Size(max = 30) String workPhone,
        @Size(max = 30) String mobilePhone,
        @Size(max = 30) String secondaryPhone,
        @Email @Size(max = 255) String email,
        @Email @Size(max = 255) String secondaryEmail,
        boolean hasMedicalAid,
        @Size(max = 150) String provider,
        @Size(max = 100) String membershipNumber
) {}
