package com.example.heartrate.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.heartrate.patient.PatientResources;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class PatientControllerTest {

    private final Patient patient = new Patient();

    {
        patient.addIdentifier().setSystem(PatientResources.EHR_ID_SYSTEM).setValue("urn:uuid:abc");
    }

    /** system|value, as FHIR writes a token: how a caller holding an EHR id finds its patient. */
    @Test
    void matchesSystemAndValue() {
        assertThat(PatientController.carries(patient, PatientResources.EHR_ID_SYSTEM + "|urn:uuid:abc")).isTrue();
        assertThat(PatientController.carries(patient, "urn:other|urn:uuid:abc")).isFalse();
    }

    @Test
    void aBareValueMatchesAnySystem() {
        assertThat(PatientController.carries(patient, "urn:uuid:abc")).isTrue();
        assertThat(PatientController.carries(patient, "urn:uuid:xyz")).isFalse();
    }
}
