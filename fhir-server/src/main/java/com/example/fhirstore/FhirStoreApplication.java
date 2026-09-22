package com.example.fhirstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * A small HL7 FHIR R4 server for the administrative half of the record.
 *
 * <p>It exists to hold what openEHR has nowhere to put. An openEHR {@code EHR_STATUS} anchors a
 * record on an identifier and nothing more — no name, no birth date, no address — so the heart rate
 * monitor could point an Observation at a patient but never say who that patient was. This is where
 * that answer lives.
 *
 * <p>It stores no clinical data. Readings remain openEHR compositions in EHRbase; the two halves
 * meet only when a Bundle is assembled.
 */
@SpringBootApplication
public class FhirStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(FhirStoreApplication.class, args);
    }
}
