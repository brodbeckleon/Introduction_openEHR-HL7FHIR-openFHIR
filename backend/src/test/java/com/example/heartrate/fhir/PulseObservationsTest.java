package com.example.heartrate.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import com.example.heartrate.model.Reading;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;

class PulseObservationsTest {

    private final PulseObservations observations = new PulseObservations(FhirContext.forR4());

    private final Reading reading =
            new Reading(OffsetDateTime.of(2026, 9, 10, 7, 15, 0, 0, ZoneOffset.UTC), 58);

    /**
     * These three values are what the FHIR Connect mapping matches on. If they drift, openFHIR
     * silently produces a composition with no content instead of failing loudly.
     */
    @Test
    void carriesTheCodesTheMappingKeysOff() {
        var observation = observations.observation(reading, "demo-patient");

        assertThat(observation.getStatus().toCode()).isEqualTo("final");
        assertThat(observation.getCategoryFirstRep().getCodingFirstRep().getCode())
                .isEqualTo("vital-signs");
        assertThat(observation.getCode().getCoding())
                .anyMatch(coding -> PulseObservations.RESTING_HEART_RATE_LOINC.equals(coding.getCode()));
    }

    @Test
    void usesTheUnitTheOpenEhrArchetypeConstrainsTo() {
        var quantity = observations.observation(reading, "demo-patient").getValueQuantity();

        assertThat(quantity.getValue().doubleValue()).isEqualTo(58);
        assertThat(quantity.getCode()).isEqualTo("/min");
        assertThat(quantity.getSystem()).isEqualTo(PulseObservations.UCUM);
    }

    @Test
    void roundTripsThroughJson() {
        var encoded = observations.encode(observations.observation(reading, "demo-patient"));
        var parsed = observations.toReading(observations.parseObservation(encoded));

        assertThat(parsed.beatsPerMinute()).isEqualTo(58);
        assertThat(parsed.measuredAt().toInstant()).isEqualTo(reading.measuredAt().toInstant());
    }

    @Test
    void wrapsObservationsInTheBundleTheContextMappingExpects() {
        var bundle = observations.bundle(List.of(observations.observation(reading, "demo-patient")));

        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.COLLECTION);
        assertThat(bundle.getEntry()).hasSize(1);
        assertThat(bundle.getEntryFirstRep().getResource().fhirType()).isEqualTo("Observation");
    }
}
