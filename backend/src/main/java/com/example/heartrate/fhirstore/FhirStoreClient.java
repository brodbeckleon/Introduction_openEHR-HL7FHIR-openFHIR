package com.example.heartrate.fhirstore;

import ca.uhn.fhir.context.FhirContext;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Talks to the FHIR server holding the administrative half of the record.
 *
 * <p>It is an ordinary FHIR client: nothing here knows that the other half of a patient's record is
 * an openEHR composition. That separation is what lets each store be read without the other running.
 */
@Component
public class FhirStoreClient {

    private static final Logger log = LoggerFactory.getLogger(FhirStoreClient.class);
    private static final MediaType FHIR_JSON = MediaType.valueOf("application/fhir+json");

    private final RestClient client;
    private final FhirContext fhirContext;

    public FhirStoreClient(@Qualifier("fhirStoreRestClient") RestClient client, FhirContext fhirContext) {
        this.client = client;
        this.fhirContext = fhirContext;
    }

    /**
     * Writes a patient under an id the caller chooses, creating it when it is not there.
     *
     * <p>A PUT rather than a POST so that seeding the demo patients is idempotent: restarting this
     * service must not leave a second Demo Patient behind.
     */
    public void upsert(Patient patient) {
        client.put()
                .uri("/fhir/Patient/{id}", patient.getIdElement().getIdPart())
                .contentType(FHIR_JSON)
                .body(fhirContext.newJsonParser().encodeResourceToString(patient))
                .retrieve()
                .toBodilessEntity();
    }

    public Optional<Patient> read(String id) {
        try {
            var json = client.get()
                    .uri("/fhir/Patient/{id}", id)
                    .accept(FHIR_JSON)
                    .retrieve()
                    .body(String.class);
            return json == null
                    ? Optional.empty()
                    : Optional.of(fhirContext.newJsonParser().parseResource(Patient.class, json));
        } catch (Exception e) {
            log.debug("Patient {} is not in the FHIR store: {}", id, e.getMessage());
            return Optional.empty();
        }
    }

    /** Every patient the store holds, in the order it returns them. */
    public List<Patient> all() {
        var json = client.get()
                .uri("/fhir/Patient")
                .accept(FHIR_JSON)
                .retrieve()
                .body(String.class);
        if (json == null) {
            return List.of();
        }
        var bundle = fhirContext.newJsonParser().parseResource(Bundle.class, json);
        return bundle.getEntry().stream()
                .map(Bundle.BundleEntryComponent::getResource)
                .filter(Patient.class::isInstance)
                .map(Patient.class::cast)
                .toList();
    }
}
