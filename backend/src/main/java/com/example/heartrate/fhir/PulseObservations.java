package com.example.heartrate.fhir;

import ca.uhn.fhir.context.FhirContext;
import com.example.heartrate.config.HeartrateProperties;
import com.example.heartrate.model.Reading;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.stereotype.Component;

/**
 * Builds and reads the HL7 FHIR R4 representation of a resting heart rate.
 *
 * <p>The codes here are not cosmetic: the FHIR Connect mapping in {@code pulse.model.yaml} keys off
 * {@code status=final}, {@code category=vital-signs} and SNOMED {@code 364075005} to decide that an
 * Observation belongs in the {@code openEHR-EHR-OBSERVATION.pulse.v2} archetype.
 */
@Component
public class PulseObservations {

    public static final String LOINC = "http://loinc.org";
    public static final String UCUM = "http://unitsofmeasure.org";
    /** LOINC "Heart rate --resting": the day's resting heart rate is what this app stores. */
    public static final String RESTING_HEART_RATE_LOINC = "40443-4";
    /** openEHR's pulse archetype constrains the rate to UCUM "/min", so FHIR carries the same unit. */
    public static final String PER_MINUTE = "/min";

    private final FhirContext fhirContext;
    private final HeartrateProperties properties;

    public PulseObservations(FhirContext fhirContext, HeartrateProperties properties) {
        this.fhirContext = fhirContext;
        this.properties = properties;
    }

    public Observation observation(Reading reading) {
        var observation = new Observation();
        observation.setId(UUID.randomUUID().toString());
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.addCategory(new CodeableConcept().addCoding(new Coding()
                .setSystem("http://terminology.hl7.org/CodeSystem/observation-category")
                .setCode("vital-signs")
                .setDisplay("Vital Signs")));
        observation.setCode(new CodeableConcept().addCoding(new Coding()
                .setSystem(LOINC)
                .setCode(RESTING_HEART_RATE_LOINC)
                .setDisplay("Heart rate --resting")));
        observation.setSubject(new Reference("Patient/" + properties.patientId()));
        observation.setEffective(new DateTimeType(Date.from(reading.measuredAt().toInstant())));
        observation.setValue(new Quantity()
                .setValue(reading.beatsPerMinute())
                .setUnit(PER_MINUTE)
                .setSystem(UCUM)
                .setCode(PER_MINUTE));
        return observation;
    }

    /**
     * The FHIR Connect context mapping is anchored on a Bundle, so a single reading travels as a
     * one-entry collection.
     */
    public Bundle bundle(List<Observation> observations) {
        var bundle = new Bundle();
        bundle.setId(UUID.randomUUID().toString());
        bundle.setType(Bundle.BundleType.COLLECTION);
        observations.forEach(observation ->
                bundle.addEntry().setFullUrl("urn:uuid:" + observation.getIdElement().getIdPart())
                        .setResource(observation));
        return bundle;
    }

    public String encode(org.hl7.fhir.instance.model.api.IBaseResource resource) {
        return fhirContext.newJsonParser().encodeResourceToString(resource);
    }

    public Observation parseObservation(String json) {
        return parse(json, Observation.class);
    }

    public <T extends org.hl7.fhir.instance.model.api.IBaseResource> T parse(String json, Class<T> type) {
        return fhirContext.newJsonParser().parseResource(type, json);
    }

    /** Pulls the timestamp and rate back out of an incoming Observation. */
    public Reading toReading(Observation observation) {
        if (!observation.hasValueQuantity()) {
            throw new IllegalArgumentException("Observation has no valueQuantity");
        }
        var effective = observation.getEffectiveDateTimeType();
        if (effective == null || effective.getValue() == null) {
            throw new IllegalArgumentException("Observation has no effectiveDateTime");
        }
        return new Reading(
                effective.getValue().toInstant().atOffset(java.time.ZoneOffset.UTC),
                observation.getValueQuantity().getValue().doubleValue());
    }
}
