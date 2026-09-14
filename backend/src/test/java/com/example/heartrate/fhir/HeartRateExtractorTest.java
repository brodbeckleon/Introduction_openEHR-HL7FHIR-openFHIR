package com.example.heartrate.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import ca.uhn.fhir.context.FhirContext;
import com.example.heartrate.config.TestMessages;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;

class HeartRateExtractorTest {

    private final HeartRateExtractor extractor = new HeartRateExtractor(TestMessages.create());
    private final FhirContext fhir = FhirContext.forR4();

    private Bundle bundleOf(String... observations) {
        return fhir.newJsonParser().parseResource(Bundle.class,
                """
                {"resourceType":"Bundle","type":"collection","entry":[%s]}
                """.formatted(String.join(",", observations)));
    }

    private static String observation(String code, String unitCode) {
        return """
               {"resource":{"resourceType":"Observation","status":"final",
                 "category":[{"coding":[{"code":"vital-signs"}]}],
                 "code":{"coding":[{"system":"http://loinc.org","code":"%s"}]},
                 "effectiveDateTime":"2026-08-20T00:00:00Z",
                 "valueQuantity":{"value":42,"code":"%s"}}}
               """.formatted(code, unitCode);
    }

    @Test
    void acceptsTheCanonicalRestingHeartRate() {
        var extraction = extractor.fromBundle(bundleOf(observation("40443-4", "/min")));

        assertThat(extraction.readings()).hasSize(1);
        assertThat(extraction.readings().get(0).beatsPerMinute()).isEqualTo(42);
        assertThat(extraction.rejections()).isEmpty();
    }

    /** Rejecting an Observation for spelling the unit the other correct way would be a bug. */
    @Test
    void acceptsTheOtherCorrectSpellingsOfBeatsPerMinute() {
        for (var unit : new String[] {"{beats}/min", "bpm", "1/min"}) {
            assertThat(extractor.fromBundle(bundleOf(observation("40443-4", unit))).readings())
                    .as("unit %s", unit)
                    .hasSize(1);
        }
    }

    /**
     * Quantity.code is optional in FHIR: a resource may carry only the human-readable unit. Looking
     * the missing half up in a Set.of() used to throw rather than answer.
     */
    @Test
    void acceptsAQuantityThatOnlyNamesTheUnit() {
        var extraction = extractor.fromBundle(bundleOf("""
                {"resource":{"resourceType":"Observation","status":"final",
                  "category":[{"coding":[{"code":"vital-signs"}]}],
                  "code":{"coding":[{"system":"http://snomed.info/sct","code":"444981005"}]},
                  "effectiveDateTime":"2026-08-20T00:00:00Z",
                  "valueQuantity":{"value":63,"unit":"bpm"}}}
                """));

        assertThat(extraction.readings()).hasSize(1);
        assertThat(extraction.readings().get(0).beatsPerMinute()).isEqualTo(63);
    }

    @Test
    void explainsWhatItSkipped() {
        var extraction = extractor.fromBundle(bundleOf(
                observation("8867-4", "/min"),
                """
                {"resource":{"resourceType":"Immunization","status":"completed",
                  "vaccineCode":{"text":"Boostrix"},"patient":{"reference":"Patient/1"}}}
                """));

        assertThat(extraction.readings()).isEmpty();
        assertThat(extraction.rejections()).hasSize(2);
        assertThat(extraction.rejections()).anyMatch(r -> r.resourceType().equals("Immunization"));
        assertThat(extraction.rejections()).anyMatch(r -> r.reason().contains("40443-4"));
    }
}
