package com.syncpoint.archive.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class AccessTest {

    private static AppUserDetails patient(long patientId) {
        return new AppUserDetails(10, "pat", "PATIENT", patientId, null, null, 1, "Pat");
    }

    private static AppUserDetails staff() {
        return new AppUserDetails(20, "staff", "STAFF", null, 7L, null, 2, "Sam Staff");
    }

    @Test
    void patientCanReadOwnRecordsOnly() {
        assertDoesNotThrow(() -> Access.requirePatientAccess(patient(5), 5));
        assertThrows(ResponseStatusException.class, () -> Access.requirePatientAccess(patient(5), 6));
    }

    @Test
    void staffCanReadAnyPatient() {
        assertDoesNotThrow(() -> Access.requirePatientAccess(staff(), 99));
    }

    @Test
    void patientAlwaysActsOnThemselves() {
        assertEquals(5, Access.patientIdFor(patient(5), null));
        assertEquals(5, Access.patientIdFor(patient(5), 5L));
        assertThrows(ResponseStatusException.class, () -> Access.patientIdFor(patient(5), 6L));
    }

    @Test
    void staffMustNameThePatient() {
        assertEquals(9, Access.patientIdFor(staff(), 9L));
        assertThrows(ResponseStatusException.class, () -> Access.patientIdFor(staff(), null));
    }
}
