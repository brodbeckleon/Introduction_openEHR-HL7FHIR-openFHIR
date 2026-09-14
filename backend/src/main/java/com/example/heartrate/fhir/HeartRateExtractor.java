package com.example.heartrate.fhir;

import com.example.heartrate.config.Messages;
import com.example.heartrate.model.Reading;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

/**
 * Decides which resources in an incoming FHIR Bundle are resting heart rates.
 *
 * <p>Real FHIR in the wild codes the unit in several equally correct ways — {@code /min},
 * {@code {beats}/min}, {@code bpm}. The FHIR Connect mapping matches on exactly one combination, so
 * anything recognised here is rebuilt into that canonical form before it is handed to openFHIR.
 * Rejecting a valid Observation because it used the other correct spelling would be a bug, not
 * strictness.
 */
@Component
public class HeartRateExtractor {

    /** LOINC 40443-4 is "Heart rate --resting"; the SNOMED codes are accepted as equivalents. */
    private static final Set<String> RESTING_CODES = Set.of("40443-4", "444981005");

    private static final Set<String> PER_MINUTE_UNITS =
            Set.of("/min", "{beats}/min", "bpm", "beats/min", "beats per minute", "1/min");

    private final Messages messages;

    public HeartRateExtractor(Messages messages) {
        this.messages = messages;
    }

    /** What an import run found, so the caller can report it rather than fail silently. */
    public record Extraction(List<Reading> readings, List<Rejection> rejections) {}

    public record Rejection(String resourceType, String reason) {}

    public Extraction fromBundle(Bundle bundle) {
        var readings = new java.util.ArrayList<Reading>();
        var rejections = new java.util.ArrayList<Rejection>();

        for (var entry : bundle.getEntry()) {
            Resource resource = entry.getResource();
            if (resource == null) {
                continue;
            }
            if (!(resource instanceof Observation observation)) {
                rejections.add(new Rejection(resource.fhirType(), messages.get("reject.notObservation")));
                continue;
            }
            reading(observation).ifPresentOrElse(
                    readings::add,
                    () -> rejections.add(new Rejection(
                            messages.get("reject.observation"), rejectionReason(observation))));
        }
        return new Extraction(List.copyOf(readings), List.copyOf(rejections));
    }

    /** A single Observation, if it is a usable resting heart rate. */
    public Optional<Reading> reading(Observation observation) {
        if (!isRestingHeartRate(observation) || !observation.hasValueQuantity()) {
            return Optional.empty();
        }
        var quantity = observation.getValueQuantity();
        if (quantity.getValue() == null || !isPerMinute(quantity.getCode(), quantity.getUnit())) {
            return Optional.empty();
        }
        var effective = observation.getEffectiveDateTimeType();
        if (effective == null || effective.getValue() == null) {
            return Optional.empty();
        }
        return Optional.of(new Reading(
                effective.getValue().toInstant().atOffset(ZoneOffset.UTC),
                quantity.getValue().doubleValue()));
    }

    private boolean isRestingHeartRate(Observation observation) {
        return observation.getCode().getCoding().stream()
                .anyMatch(coding -> RESTING_CODES.contains(coding.getCode()));
    }

    private boolean isPerMinute(String code, String unit) {
        // An Observation that omits the unit entirely is taken at face value: a heart rate has no
        // other plausible unit. Either half may be absent on its own — Quantity.code is optional —
        // and Set.of() throws on a null lookup rather than answering false.
        if (code == null && unit == null) {
            return true;
        }
        return (code != null && PER_MINUTE_UNITS.contains(code))
                || (unit != null && PER_MINUTE_UNITS.contains(unit));
    }

    /** Why this Observation is not a usable resting heart rate — the pipeline inspector shows it. */
    public String rejectionReason(Observation observation) {
        if (!isRestingHeartRate(observation)) {
            return messages.get("reject.notResting");
        }
        if (!observation.hasValueQuantity() || observation.getValueQuantity().getValue() == null) {
            return messages.get("reject.noValue");
        }
        var quantity = observation.getValueQuantity();
        if (!isPerMinute(quantity.getCode(), quantity.getUnit())) {
            return messages.get("reject.unit",
                    quantity.getCode() != null ? quantity.getCode() : quantity.getUnit());
        }
        return messages.get("reject.noTime");
    }
}
