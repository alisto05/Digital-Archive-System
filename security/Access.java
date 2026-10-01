package com.syncpoint.archive.security;

import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

/** Ownership rules: a patient can only touch their own records; staff can act on any patient. */
public final class Access {

    private Access() {
    }

    /** Throws 403 when a patient asks for someone else's data. */
    public static void requirePatientAccess(AppUserDetails me, long patientId) {
        if (me.isPatient() && (me.patientId() == null || me.patientId() != patientId)) {
            throw new ResponseStatusException(FORBIDDEN, "You can only access your own records.");
        }
    }

    /**
     * Which patient a create-request is for. Patients always act on themselves
     * (a different patientId is rejected); staff must say which patient.
     */
    public static long patientIdFor(AppUserDetails me, Long requestedPatientId) {
        if (me.isPatient()) {
            if (me.patientId() == null
                    || (requestedPatientId != null && !requestedPatientId.equals(me.patientId()))) {
                throw new ResponseStatusException(FORBIDDEN, "You can only act on your own records.");
            }
            return me.patientId();
        }
        if (requestedPatientId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "patientId is required.");
        }
        return requestedPatientId;
    }
}
