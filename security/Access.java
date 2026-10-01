package com.syncpoint.archive.security;

import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;


public final class Access {

    private Access() {
    }

   
    public static void requirePatientAccess(AppUserDetails me, long patientId) {
        if (me.isPatient() && (me.patientId() == null || me.patientId() != patientId)) {
            throw new ResponseStatusException(FORBIDDEN, "You can only access your own records.");
        }
    }

   
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
