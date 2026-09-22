package com.example.heartrate.trace;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * One stage of the import pipeline, kept so the UI can show what the data looked like there.
 *
 * @param id stable identifier the frontend keys off
 * @param title what this stage is called in the UI
 * @param actor who produced this representation — the backend, openFHIR or EHRbase
 * @param standard {@code none}, {@code fhir} or {@code openehr}: which model the JSON is written in
 * @param call the HTTP call that produced it, when a server was involved
 * @param explanation why this stage exists — the teaching text
 * @param note a caveat about this particular run, e.g. which day of a multi-day file was followed
 * @param status {@code ok} or {@code error}
 * @param json the representation itself
 * @param durationMs how long the call took, when there was one
 * @param differences only on the round trip: what the journey through openEHR changed
 * @param query the AQL, kept out of the JSON so the UI can show it as the query it is rather than
 *     as one escaped string
 * @param direction {@code in} for the way a reading travels into the record, {@code out} for the way
 *     it comes back. The inspector shows one at a time: they are two operations — a POST and a GET —
 *     and drawing them as one line was what made a lookup look like a link in a chain.
 */
public record TraceStep(
        String id,
        String title,
        String actor,
        String standard,
        String call,
        String explanation,
        String note,
        String status,
        JsonNode json,
        Long durationMs,
        List<RoundTripDifference> differences,
        String query,
        String direction) {

    static TraceStep of(String id, String title, String actor, String standard, String explanation, JsonNode json) {
        return new TraceStep(
                id, title, actor, standard, null, explanation, null, "ok", json, null, null, null, "in");
    }

    TraceStep withCall(String call, long durationMs) {
        return new TraceStep(
                id, title, actor, standard, call, explanation, note, status, json, durationMs, differences, query,
                direction);
    }

    TraceStep withNote(String note) {
        return new TraceStep(
                id, title, actor, standard, call, explanation, note, status, json, durationMs, differences, query,
                direction);
    }

    TraceStep withDifferences(List<RoundTripDifference> differences) {
        return new TraceStep(
                id, title, actor, standard, call, explanation, note, status, json, durationMs, differences, query,
                direction);
    }

    TraceStep withQuery(String query) {
        return new TraceStep(
                id, title, actor, standard, call, explanation, note, status, json, durationMs, differences, query,
                direction);
    }

    TraceStep withJson(JsonNode json) {
        return new TraceStep(
                id, title, actor, standard, call, explanation, note, status, json, durationMs, differences, query,
                direction);
    }

    /** Moves this stage onto the way back — the GET rather than the POST. */
    TraceStep onTheWayBack() {
        return new TraceStep(
                id, title, actor, standard, call, explanation, note, status, json, durationMs, differences, query,
                "out");
    }

    static TraceStep failed(String id, String title, String actor, String explanation, String reason) {
        return new TraceStep(
                id, title, actor, "none", null, explanation, reason, "error", null, null, null, null, "in");
    }
}
