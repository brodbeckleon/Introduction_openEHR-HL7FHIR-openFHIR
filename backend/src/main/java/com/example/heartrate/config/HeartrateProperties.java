package com.example.heartrate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Everything that points this service at the two standards servers it talks to.
 */
@ConfigurationProperties(prefix = "heartrate")
public record HeartrateProperties(
        Ehrbase ehrbase,
        OpenFhir openfhir,
        /** Template the compositions are built against; must match the .opt shipped to both servers. */
        String templateId,
        /** Fixed EHR the demo writes to, so restarts keep reading the same record. */
        String ehrId,
        /** Subject the FHIR Observations reference. */
        String patientId,
        String composerName,
        String territory,
        /**
         * Where the FHIR Connect mappings live, so the pipeline inspector can show the rule behind a
         * mapping. Read live when the directory is reachable; the copy on the classpath is the
         * fallback for running from a jar.
         */
        String mappingsDir) {

    public record Ehrbase(String baseUrl, String username, String password) {}

    public record OpenFhir(String baseUrl) {}
}
