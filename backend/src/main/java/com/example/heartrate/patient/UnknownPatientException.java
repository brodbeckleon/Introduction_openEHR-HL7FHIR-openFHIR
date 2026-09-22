package com.example.heartrate.patient;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Asked for a patient this instance does not know. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class UnknownPatientException extends RuntimeException {

    public UnknownPatientException(String patientId) {
        super("Unknown patient: " + patientId);
    }
}
