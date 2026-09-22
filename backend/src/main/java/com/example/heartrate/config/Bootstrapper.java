package com.example.heartrate.config;

import com.example.heartrate.fhirstore.FhirStoreClient;
import com.example.heartrate.openehr.EhrbaseClient;
import com.example.heartrate.patient.EhrResolver;
import com.example.heartrate.patient.PatientDirectory;
import com.example.heartrate.patient.PatientResources;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Prepares both halves of the record before the first reading arrives.
 *
 * <p>openEHR gets the operational template and an EHR per patient; the FHIR store gets the patient
 * themselves, carrying the EHR id as a secondary identifier. That identifier is the only thing
 * connecting the two, and writing it here is what makes the connection exist.
 *
 * <p>Each half is prepared independently and a failure in one is logged rather than thrown: a
 * missing FHIR store should cost the demo its names, not its chart.
 */
@Component
public class Bootstrapper implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(Bootstrapper.class);

    private final EhrbaseClient ehrbase;
    private final PatientDirectory directory;
    private final EhrResolver ehrResolver;
    private final PatientResources resources;
    private final FhirStoreClient fhirStore;

    public Bootstrapper(
            EhrbaseClient ehrbase,
            PatientDirectory directory,
            EhrResolver ehrResolver,
            PatientResources resources,
            FhirStoreClient fhirStore) {
        this.ehrbase = ehrbase;
        this.directory = directory;
        this.ehrResolver = ehrResolver;
        this.resources = resources;
        this.fhirStore = fhirStore;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            var opt = new ClassPathResource("heartrate_monitor.opt")
                    .getContentAsString(StandardCharsets.UTF_8);
            ehrbase.uploadTemplate(opt);
        } catch (Exception e) {
            log.error("Could not prepare EHRbase — is `docker compose up` running? ({})", e.getMessage());
            return;
        }

        for (var patient : directory.roster()) {
            String ehrId;
            try {
                ehrId = ehrResolver.ehrIdFor(patient.id());
            } catch (Exception e) {
                log.error("Could not resolve an EHR for {}: {}", patient.id(), e.getMessage());
                continue;
            }
            log.info("Patient {} uses EHR {}", patient.id(), ehrId);
            seedIntoFhirStore(patient, ehrId);
        }
    }

    /**
     * Writes the patient only when the store does not have them yet.
     *
     * <p>Overwriting on every start would undo anything edited through the FHIR API, and the store —
     * not this configuration — is where a patient's details are supposed to live once they exist.
     */
    private void seedIntoFhirStore(HeartrateProperties.Patient patient, String ehrId) {
        try {
            if (fhirStore.read(patient.id()).isPresent()) {
                return;
            }
            fhirStore.upsert(resources.toFhir(patient, ehrId));
            log.info("Seeded patient {} into the FHIR store", patient.id());
        } catch (Exception e) {
            log.warn("Could not seed {} into the FHIR store — names will be missing until it is "
                    + "reachable ({})", patient.id(), e.getMessage());
        }
    }
}
