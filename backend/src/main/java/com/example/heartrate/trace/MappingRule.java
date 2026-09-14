package com.example.heartrate.trace;

/**
 * One rule read out of a FHIR Connect file: what it connects, and where it is written.
 *
 * <p>The pipeline inspector shows correspondences for one traced reading, anchored in that reading's
 * JSON. These are the same relationships read from the mapping instead — so the list changes when
 * the file is edited, and it stands on its own without a trace to hang from.
 *
 * @param kind {@code correspondence} — connects a FHIR path to an openEHR path; {@code constant} —
 *     writes a fixed value into outgoing FHIR, which is how an exported Observation gets a LOINC
 *     code that openEHR never stored
 * @param depth how deeply the rule is nested; a nested rule only applies inside its parent
 * @param fhir the FHIR path, or the path a constant is written to
 * @param openehr the openEHR path, absent on a constant
 * @param value the fixed value, only on a constant
 */
public record MappingRule(
        String file,
        String name,
        String kind,
        int depth,
        String fhir,
        String openehr,
        String value,
        int fromLine,
        int toLine) {}
