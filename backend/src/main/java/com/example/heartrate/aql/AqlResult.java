package com.example.heartrate.aql;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * What one query in the playground produced.
 *
 * @param columns the result set's own column metadata — the AQL path behind each name, which is the
 *     part that explains why a column is called what it is
 * @param rows the values, already flattened to a list per row
 * @param truncated whether more rows existed than were returned
 * @param error the store's complaint, when the query was refused
 */
public record AqlResult(
        List<Column> columns,
        List<List<JsonNode>> rows,
        int returned,
        boolean truncated,
        long durationMs,
        String error) {

    /**
     * @param path the AQL path this column selects — absent for an aggregate
     * @param name the alias, or one EHRbase made up
     */
    public record Column(String path, String name) {}

    static AqlResult failed(String error, long durationMs) {
        return new AqlResult(List.of(), List.of(), 0, false, durationMs, error);
    }
}
