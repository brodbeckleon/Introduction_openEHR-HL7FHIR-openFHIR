package com.example.heartrate.config;

import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything that points this service at the two standards servers it talks to.
 *
 * <p>The third store, the patients' own database, is configured as an ordinary Spring datasource
 * rather than here: it is not a server this service talks to but its own.
 */
@ConfigurationProperties(prefix = "heartrate")
public record HeartrateProperties(
        Ehrbase ehrbase,
        OpenFhir openfhir,
        /** Template the compositions are built against; must match the .opt given to both servers. */
        String templateId,
        /** The patient a request is about when it names none. */
        String defaultPatient,
        /**
         * The patients this instance knows. Each one gets an EHR of its own, found by the id rather
         * than configured: see {@link com.example.heartrate.patient.EhrResolver}.
         */
        List<Patient> patients,
        String composerName,
        String territory,
        /**
         * Where the FHIR Connect mappings and the operational template live. This service is their
         * only reader: it hands them to openFHIR and the template to EHRbase, and the pipeline
         * inspector shows the rule behind a mapping from the same files. Read live when the
         * directory is reachable; the copy on the classpath is the fallback for running from a jar.
         */
        String mappingsDir) {

    public record Ehrbase(String baseUrl, String username, String password) {}

    public record OpenFhir(String baseUrl) {}

    /**
     * A demo patient, as this instance seeds it into the FHIR store on first start.
     *
     * <p>Only the id reaches openEHR — that is all {@code EHR_STATUS.subject} holds. Everything
     * below it is what the FHIR store exists for: a record openEHR has nowhere to put.
     */
    public record Patient(
            String id, String name, LocalDate birthDate, String gender, Address address) {}

    public record Address(String line, String postalCode, String city, String country) {}
}
