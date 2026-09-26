package com.example.heartrate.trace;

/**
 * One field that did not survive the trip through openEHR unchanged.
 *
 * <p>This is the point of the round trip: openEHR stores what the archetype models, so anything FHIR
 * carried beyond that — the coding system, a display name — has nowhere to live and does not come
 * back. Measured against what the export serves, not against openFHIR's raw answer: the id and the
 * subject are put back by the backend, and reporting them lost would blame the model for a gap the
 * service fills.
 *
 * @param pointer JSON pointer into the Bundle, e.g. {@code /entry/0/resource/subject/reference}
 * @param kind {@code lost}, {@code changed} or {@code added}
 */
public record RoundTripDifference(String pointer, String kind, String before, String after) {}
