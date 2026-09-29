package com.example.heartrate.trace;

import java.util.List;

/**
 * One run of the import pipeline with every intermediate representation kept.
 *
 * <p>A run never writes: everything here was produced by reads and by openFHIR, which is what makes
 * tracing the same reading twenty times harmless.
 *
 * @param inputKind {@code bundle} or {@code observation}
 * @param inputLabel how the UI announces what was recognised
 * @param steps the representations, in the order they were produced
 * @param links the FHIR ⇄ openEHR correspondences, all applying to the {@code composition} step
 * @param mappings the FHIR Connect files, so the UI can show the rule that produced a link
 */
public record Trace(
        String inputKind,
        String inputLabel,
        List<TraceStep> steps,
        List<MappingLink> links,
        List<MappingSource> mappings) {}
