package com.example.heartrate.trace;

/**
 * One correspondence between a place in the FHIR JSON and a place in the openEHR COMPOSITION,
 * together with the FHIR Connect rule that put it there.
 *
 * <p>Clicking one in the UI highlights all three at once, which is the shortest available
 * explanation of what a mapping engine actually does.
 *
 * @param kind {@code mapped} — a rule copies a value across; {@code selector} — the FHIR value
 *     chooses a structure rather than becoming data; {@code generated} — openEHR requires it and
 *     openFHIR supplies it, because FHIR never carried it
 * @param fhirPointer JSON pointer into the Bundle handed to openFHIR, or null
 * @param openehrPointer JSON pointer into the COMPOSITION openFHIR produced, or null
 * @param mappingFile which file in {@code openfhir-bootstrap/} carries the rule, or null
 * @param fromLine first line of the rule, 1-based
 * @param toLine last line of the rule, 1-based
 */
public record MappingLink(
        String id,
        String label,
        String kind,
        String explanation,
        String fhirPointer,
        String openehrPointer,
        String mappingFile,
        Integer fromLine,
        Integer toLine) {}
