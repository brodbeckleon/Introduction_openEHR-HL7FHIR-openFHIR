package com.example.heartrate.aql;

import com.example.heartrate.config.HeartrateProperties;
import com.example.heartrate.config.Messages;
import com.example.heartrate.openehr.EhrbaseClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

/**
 * Runs a query someone typed, against the real record.
 *
 * <p>AQL is what makes openEHR queryable without knowing its storage: it selects by archetype path,
 * so a query written against the pulse archetype runs on any openEHR system that knows that
 * archetype. Reading about that is not the same as typing a path wrong and seeing what comes back.
 *
 * <p>AQL is a query language with no write operations at all — there is no AQL statement that
 * changes anything. The guard below is therefore not what makes this safe, it is what keeps the
 * error message useful when someone pastes SQL out of habit.
 */
@Service
public class AqlService {

    /** Enough to see the shape of an answer without turning the browser into a spreadsheet. */
    private static final int MAX_ROWS = 200;

    private final EhrbaseClient ehrbase;
    private final HeartrateProperties properties;
    private final Messages messages;
    private final ObjectMapper objectMapper;

    public AqlService(
            EhrbaseClient ehrbase,
            HeartrateProperties properties,
            Messages messages,
            ObjectMapper objectMapper) {
        this.ehrbase = ehrbase;
        this.properties = properties;
        this.messages = messages;
        this.objectMapper = objectMapper;
    }

    public AqlResult run(String query) {
        var trimmed = query == null ? "" : query.strip();
        if (trimmed.isEmpty()) {
            return AqlResult.failed(messages.get("aql.empty"), 0);
        }
        if (!trimmed.regionMatches(true, 0, "SELECT", 0, "SELECT".length())) {
            return AqlResult.failed(messages.get("aql.notSelect"), 0);
        }

        long started = System.nanoTime();
        JsonNode answer;
        try {
            // $ehrId is filled in rather than demanded: the demo has one EHR, and making people
            // paste a uuid before their first query would teach them nothing about AQL.
            answer = ehrbase.queryRaw(trimmed, Map.of("ehrId", properties.ehrId()));
        } catch (Exception e) {
            return AqlResult.failed(explain(e), millisSince(started));
        }
        long took = millisSince(started);

        var columns = new ArrayList<AqlResult.Column>();
        for (JsonNode column : answer.path("columns")) {
            columns.add(new AqlResult.Column(
                    column.path("path").asText(null), column.path("name").asText(null)));
        }

        var rows = new ArrayList<List<JsonNode>>();
        var all = answer.path("rows");
        for (JsonNode row : all) {
            if (rows.size() >= MAX_ROWS) {
                break;
            }
            var cells = new ArrayList<JsonNode>();
            row.forEach(cells::add);
            rows.add(List.copyOf(cells));
        }

        return new AqlResult(
                List.copyOf(columns), List.copyOf(rows), all.size(), all.size() > rows.size(), took, null);
    }

    /**
     * The complaint someone can act on.
     *
     * <p>A rejected query arrives as an HTTP error whose message is the status line wrapped around a
     * JSON body. The body's {@code message} is the part that says what is wrong with the query —
     * "mismatched input 'WHERE'" — and the rest is noise in front of it.
     */
    private String explain(Throwable error) {
        if (error instanceof RestClientResponseException http) {
            try {
                var body = objectMapper.readTree(http.getResponseBodyAsString());
                var message = body.path("message").asText(null);
                if (message != null && !message.isBlank()) {
                    return message;
                }
            } catch (Exception ignored) {
                // Fall through to the exception's own message.
            }
        }
        var cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        var message = cause.getMessage();
        return message == null || message.isBlank() ? error.toString() : message;
    }

    private static long millisSince(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }
}
