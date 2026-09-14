package com.example.heartrate.traffic;

/**
 * One HTTP call this service made to a standards server.
 *
 * <p>No headers are kept. That is partly why — EHRbase runs with basic auth and its credentials
 * have no business being in a buffer the browser can read — and partly because the bodies are the
 * part worth looking at: an openEHR COMPOSITION going in, an AQL result set coming back.
 *
 * @param seq monotonic id, so the browser can ask for what it has not seen yet
 * @param server {@code openFHIR} or {@code EHRbase}
 * @param status null when the call never got an answer
 * @param error the failure, when there was one
 */
public record TrafficEntry(
        long seq,
        String at,
        String server,
        String method,
        String path,
        String query,
        Integer status,
        long durationMs,
        String requestBody,
        String responseBody,
        String error) {}
