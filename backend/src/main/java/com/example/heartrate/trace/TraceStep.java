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
 * @param explanation why this stage exists — the teaching text, in full
 * @param summary the same in a sentence or two, shown first; the full text is one click away. Only
 *     where the full text is long enough to need it, so it is often null
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
 * @param branch whether this stage hangs off the line rather than lying on it. The FHIR store is the
 *     only thing that does: the reading never passes through it, so a Patient drawn in the chain
 *     would claim a descent that does not exist. It hangs off the stage before it.
 */
public record TraceStep(
        String id,
        String title,
        String actor,
        String standard,
        String call,
        String explanation,
        String summary,
        String note,
        String status,
        JsonNode json,
        Long durationMs,
        List<RoundTripDifference> differences,
        String query,
        String direction,
        boolean branch) {

    static TraceStep of(String id, String title, String actor, String standard, String explanation, JsonNode json) {
        return new TraceStep(
                id, title, actor, standard, null, explanation, null, null, "ok", json, null, null, null, "in", false);
    }

    TraceStep withCall(String call, long durationMs) {
        return new TraceStep(
                id, title, actor, standard, call, explanation, summary, note, status, json, durationMs, differences,
                query, direction, branch);
    }

    TraceStep withNote(String note) {
        return new TraceStep(
                id, title, actor, standard, call, explanation, summary, note, status, json, durationMs, differences,
                query, direction, branch);
    }

    TraceStep withSummary(String summary) {
        return new TraceStep(
                id, title, actor, standard, call, explanation, summary, note, status, json, durationMs, differences,
                query, direction, branch);
    }

    TraceStep withDifferences(List<RoundTripDifference> differences) {
        return new TraceStep(
                id, title, actor, standard, call, explanation, summary, note, status, json, durationMs, differences,
                query, direction, branch);
    }

    TraceStep withQuery(String query) {
        return new TraceStep(
                id, title, actor, standard, call, explanation, summary, note, status, json, durationMs, differences,
                query, direction, branch);
    }

    TraceStep withJson(JsonNode json) {
        return new TraceStep(
                id, title, actor, standard, call, explanation, summary, note, status, json, durationMs, differences,
                query, direction, branch);
    }

    /** Moves this stage onto the way back — the GET rather than the POST. */
    TraceStep onTheWayBack() {
        return new TraceStep(
                id, title, actor, standard, call, explanation, summary, note, status, json, durationMs, differences,
                query, "out", branch);
    }

    /** Hangs this stage off the line rather than putting it on it. */
    TraceStep asBranch() {
        return new TraceStep(
                id, title, actor, standard, call, explanation, summary, note, status, json, durationMs, differences,
                query, direction, true);
    }

    static TraceStep failed(String id, String title, String actor, String explanation, String reason) {
        return new TraceStep(
                id, title, actor, "none", null, explanation, null, reason, "error", null, null, null, null, "in",
                false);
    }
}
