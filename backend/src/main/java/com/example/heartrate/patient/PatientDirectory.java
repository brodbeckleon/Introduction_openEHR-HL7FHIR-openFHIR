package com.example.heartrate.patient;

import com.example.heartrate.config.HeartrateProperties;
import com.example.heartrate.fhirstore.FhirStoreClient;
import java.util.List;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The patients this instance knows, assembled from the two places that each hold half the answer.
 *
 * <p>Three stores, three questions:
 *
 * <ul>
 *   <li>configuration says <em>which</em> patients this demo has — a fixed roster, so validating an
 *       incoming id costs nothing and works with both servers down;
 *   <li>the FHIR store says <em>who they are</em> — name, birth date, gender, address;
 *   <li>openEHR says <em>what was measured</em>, and anchors its record on the id alone.
 * </ul>
 *
 * <p>Keeping the roster in configuration rather than reading it from the FHIR store is deliberate.
 * The administrative half being unavailable should not take the clinical half down with it: the
 * chart still draws, the AQL playground still answers, and only the names go missing.
 */
@Component
public class PatientDirectory {

    private static final Logger log = LoggerFactory.getLogger(PatientDirectory.class);

    private final HeartrateProperties properties;
    private final FhirStoreClient store;
    private final PatientResources resources;
    private final EhrResolver ehrResolver;

    public PatientDirectory(
            HeartrateProperties properties,
            FhirStoreClient store,
            PatientResources resources,
            EhrResolver ehrResolver) {
        this.properties = properties;
        this.store = store;
        this.resources = resources;
        this.ehrResolver = ehrResolver;
    }

    /** The roster: which patients exist, regardless of what either server has to say about them. */
    public List<HeartrateProperties.Patient> roster() {
        return properties.patients() == null ? List.of() : properties.patients();
    }

    /**
     * Every patient as FHIR, read from the store.
     *
     * <p>Falls back to projecting the roster when the store cannot be reached, so the patient
     * switcher keeps working. What that fallback shows is the seed this service started from, not
     * whatever the store holds now — an address corrected through the FHIR API is exactly what goes
     * missing, and it comes back when the store does.
     */
    public List<Patient> all() {
        try {
            var stored = store.all();
            if (!stored.isEmpty()) {
                return stored;
            }
            log.debug("The FHIR store holds no patients yet; projecting the roster instead");
        } catch (Exception e) {
            log.warn("Could not reach the FHIR store, falling back to the roster: {}", e.getMessage());
        }
        return roster().stream().map(this::project).toList();
    }

    public Patient byId(String patientId) {
        var configured = configured(patientId);
        return store.read(patientId).orElseGet(() -> project(configured));
    }

    /**
     * The patient a request is about.
     *
     * <p>Requests name the patient, so the value arrives from outside and is checked against the
     * roster before it reaches an EHR lookup. Without that check any id at all would create a
     * record on first use.
     *
     * @param requested what the request asked for, or {@code null} for the configured default
     */
    public String resolve(String requested) {
        if (requested == null || requested.isBlank()) {
            return properties.defaultPatient();
        }
        return configured(requested).id();
    }

    private HeartrateProperties.Patient configured(String patientId) {
        return roster().stream()
                .filter(patient -> patient.id().equals(patientId))
                .findFirst()
                .orElseThrow(() -> new UnknownPatientException(patientId));
    }

    private Patient project(HeartrateProperties.Patient patient) {
        return resources.toFhir(patient, ehrResolver.ehrIdFor(patient.id()));
    }
}
