package com.example.heartrate.traffic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * A short memory of the calls this service made to openFHIR and EHRbase.
 *
 * <p>Reading about the openEHR REST API is one thing; watching a COMPOSITION be POSTed to
 * {@code /ehr/{id}/composition} and come back with a version uid is another. The buffer is small and
 * in memory on purpose — this is a window onto what just happened, not an audit log.
 */
@Component
public class TrafficRecorder {

    /** Enough to cover a full import of the sample file without growing without bound. */
    private static final int CAPACITY = 300;

    /** The operational template alone is 48 KB of XML; nobody reads that in a browser panel. */
    private static final int MAX_BODY = 16 * 1024;

    private final Deque<TrafficEntry> entries = new ConcurrentLinkedDeque<>();
    private final AtomicLong sequence = new AtomicLong();

    public void record(
            String server,
            String method,
            String path,
            String query,
            Integer status,
            long durationMs,
            String requestBody,
            String responseBody,
            String error) {
        entries.addLast(new TrafficEntry(
                sequence.incrementAndGet(),
                Instant.now().toString(),
                server,
                method,
                path,
                query,
                status,
                durationMs,
                truncate(requestBody),
                truncate(responseBody),
                error));
        while (entries.size() > CAPACITY) {
            entries.pollFirst();
        }
    }

    /** Everything recorded after {@code seq}, oldest first. */
    public List<TrafficEntry> since(long seq) {
        var recent = new ArrayList<TrafficEntry>();
        for (var entry : entries) {
            if (entry.seq() > seq) {
                recent.add(entry);
            }
        }
        return List.copyOf(recent);
    }

    public void clear() {
        entries.clear();
    }

    private static String truncate(String body) {
        if (body == null || body.length() <= MAX_BODY) {
            return body;
        }
        return body.substring(0, MAX_BODY) + "\n… truncated, %d characters in total".formatted(body.length());
    }
}
