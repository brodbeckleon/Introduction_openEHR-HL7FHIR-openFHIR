package com.example.heartrate.patient;

import com.example.heartrate.config.HeartrateProperties;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The patients this instance knows about.
 *
 * <p>A demo directory read from configuration, and deliberately the thinnest thing that works: a
 * patient here is an id, a name and a birth date. There is nowhere to put an address, an insurance
 * number or a second identifier — which is the honest limit of keeping demographics in openEHR's
 * {@code EHR_STATUS} and the reason a FHIR store earns its place once you need more.
 */
@Component
public class PatientDirectory {

    private final HeartrateProperties properties;

    public PatientDirectory(HeartrateProperties properties) {
        this.properties = properties;
    }

    public List<HeartrateProperties.Patient> all() {
        return properties.patients() == null ? List.of() : properties.patients();
    }

    public HeartrateProperties.Patient byId(String patientId) {
        return all().stream()
                .filter(patient -> patient.id().equals(patientId))
                .findFirst()
                .orElseThrow(() -> new UnknownPatientException(patientId));
    }

    /**
     * The patient a request is about.
     *
     * <p>Requests name the patient, so the value arrives from outside and is checked against the
     * directory before it reaches an EHR lookup. Without that check any id at all would create a
     * record on first use.
     *
     * @param requested what the request asked for, or {@code null} for the configured default
     */
    public String resolve(String requested) {
        if (requested == null || requested.isBlank()) {
            return properties.defaultPatient();
        }
        return byId(requested).id();
    }
}
