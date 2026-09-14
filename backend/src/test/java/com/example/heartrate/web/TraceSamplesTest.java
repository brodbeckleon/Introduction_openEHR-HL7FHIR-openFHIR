package com.example.heartrate.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class TraceSamplesTest {

    private JsonNode samples() throws Exception {
        return new ObjectMapper().readTree(
                new ClassPathResource("trace-samples.json").getContentAsString(StandardCharsets.UTF_8));
    }

    private static JsonNode observationIn(JsonNode node) {
        if (node.isObject() && "Observation".equals(node.path("resourceType").asText())) {
            return node;
        }
        for (JsonNode child : node) {
            var found = observationIn(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    /**
     * Dates are placeholders, not literals. A sample dated last March is a sample about a day the
     * chart no longer shows — the same trap a checked-in sample file falls into.
     */
    @Test
    void carriesNoFixedDates() throws Exception {
        for (JsonNode sample : samples()) {
            var when = observationIn(sample.path("json")).path("effectiveDateTime").asText();
            assertThat(when).as("%s", sample.path("id").asText()).matches("\\$\\{(today|yesterday)}");
        }
    }

    /**
     * The tour says "one number: 58" over the entry form, then traces the first sample. If the two
     * disagree the reader is told to follow a number that changes under them.
     */
    @Test
    void theFirstSampleCarriesTheNumberTheTourNames() throws Exception {
        var first = samples().get(0);

        assertThat(first.path("id").asText()).isEqualTo("bundle");
        var observation = observationIn(first.path("json"));
        assertThat(observation.path("valueQuantity").path("value").asInt()).isEqualTo(58);
        assertThat(observation.path("effectiveDateTime").asText()).isEqualTo("${today}");
    }

    @Test
    void everySampleIsRecognisablyDistinct() throws Exception {
        var values = new java.util.ArrayList<Integer>();
        for (JsonNode sample : samples()) {
            values.add(observationIn(sample.path("json")).path("valueQuantity").path("value").asInt());
        }
        assertThat(values).doesNotHaveDuplicates();
    }
}
