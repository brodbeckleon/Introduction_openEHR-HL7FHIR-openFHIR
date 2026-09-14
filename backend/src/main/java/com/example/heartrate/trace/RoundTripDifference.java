package com.example.heartrate.trace;

/**
 * One field that did not survive the trip through openEHR unchanged.
 *
 * <p>This is the point of the round trip: openEHR stores what the archetype models, so anything FHIR
 * carried beyond that — the coding system, the subject reference, a display name — has nowhere to
 * live and does not come back.
 *
 * @param pointer JSON pointer into the Bundle, e.g. {@code /entry/0/resource/subject/reference}
 * @param kind {@code lost}, {@code changed} or {@code added}
 */
public record RoundTripDifference(String pointer, String kind, String before, String after) {}
