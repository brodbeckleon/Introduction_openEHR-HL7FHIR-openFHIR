package com.example.heartrate.config;

import com.example.heartrate.fhirstore.FhirStore;
import com.example.heartrate.openehr.EhrbaseClient;
import com.example.heartrate.openfhir.OpenFhirLoader;
import com.example.heartrate.patient.EhrResolver;
import com.example.heartrate.patient.PatientDirectory;
import com.example.heartrate.patient.PatientResources;
import com.example.heartrate.trace.MappingLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

/**
 * Prepares both halves of the record, and the engine between them, before the first reading arrives.
 *
 * <p>openEHR gets the operational template and an EHR per patient; the FHIR store gets the patients
 * themselves, carrying the EHR id as a secondary identifier; openFHIR gets the same template and the
 * mappings. That identifier is the only thing connecting the two halves, and writing it here is what
 * makes the connection exist. The template both servers get is one file, read once.
 *
 * <p>Each part is prepared independently and a failure in one is logged rather than thrown: a
 * missing FHIR store should cost the demo its names, not its chart.
 */
@Component
public class Bootstrapper implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(Bootstrapper.class);

    /** How long to wait for openFHIR, which compose starts but does not wait to be ready. */
    private static final int OPENFHIR_ATTEMPTS = 30;
    private static final long OPENFHIR_PAUSE_MS = 2000;

    private final EhrbaseClient ehrbase;
    private final PatientDirectory directory;
    private final EhrResolver ehrResolver;
    private final PatientResources resources;
    private final FhirStore fhirStore;
    private final MappingLibrary mappings;
    private final OpenFhirLoader openFhir;

    public Bootstrapper(
            EhrbaseClient ehrbase,
            PatientDirectory directory,
            EhrResolver ehrResolver,
            PatientResources resources,
            FhirStore fhirStore,
            MappingLibrary mappings,
            OpenFhirLoader openFhir) {
        this.ehrbase = ehrbase;
        this.directory = directory;
        this.ehrResolver = ehrResolver;
        this.resources = resources;
        this.fhirStore = fhirStore;
        this.mappings = mappings;
        this.openFhir = openFhir;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (prepareEhrbase()) {
            preparePatients();
        }
        loadOpenFhir();
    }

    private boolean prepareEhrbase() {
        try {
            ehrbase.uploadTemplate(mappings.operationalTemplate());
            return true;
        } catch (Exception e) {
            log.error("Could not prepare EHRbase — is `docker compose up` running? ({})", e.getMessage());
            return false;
        }
    }

    private void preparePatients() {
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
            log.warn("Could not seed {} into the FHIR store — names will come from the roster until it "
                    + "is reachable ({})", patient.id(), e.getMessage());
        }
    }

    /**
     * Hands openFHIR the template and the mappings, waiting for it to come up first.
     *
     * <p>Only an unreachable openFHIR is waited out. A file it refuses is logged and left: the
     * mapping editor shows the same objection the next time that file is saved.
     */
    private void loadOpenFhir() {
        for (int attempt = 1; attempt <= OPENFHIR_ATTEMPTS; attempt++) {
            try {
                openFhir.loadAll().stream()
                        .filter(outcome -> !outcome.applied())
                        .forEach(outcome -> log.error("openFHIR refused {}: {}", outcome.file(), outcome.detail()));
                return;
            } catch (ResourceAccessException e) {
                log.info("Waiting for openFHIR ({}/{}): {}", attempt, OPENFHIR_ATTEMPTS, e.getMessage());
                try {
                    Thread.sleep(OPENFHIR_PAUSE_MS);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } catch (Exception e) {
                log.error("Could not hand openFHIR the template and mappings: {}", e.getMessage());
                return;
            }
        }
        log.error("openFHIR did not answer within a minute; the mappings were not loaded. Restart this "
                + "service once it is up.");
    }
}
