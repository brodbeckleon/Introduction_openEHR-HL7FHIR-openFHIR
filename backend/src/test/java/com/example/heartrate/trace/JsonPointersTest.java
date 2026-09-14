package com.example.heartrate.trace;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class JsonPointersTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /** Shaped like what openFHIR answers with: the rate sits behind three levels of nesting. */
    private static final String COMPOSITION = """
            {"_type":"COMPOSITION",
             "content":[{"_type":"OBSERVATION",
               "data":{"events":[{"_type":"POINT_EVENT",
                 "time":{"value":"2026-09-10T00:00:00Z"},
                 "data":{"items":[{"value":{"magnitude":58.0,"units":"/min"},
                                   "archetype_node_id":"at0004"}],
                         "archetype_node_id":"at0001"},
                 "archetype_node_id":"at0003"}],
                "archetype_node_id":"at0002"},
               "archetype_node_id":"openEHR-EHR-OBSERVATION.pulse.v2"}],
             "archetype_node_id":"openEHR-EHR-COMPOSITION.encounter.v1"}
            """;

    @Test
    void locatesAnArchetypeNodeWhereverItSits() throws Exception {
        var composition = mapper.readTree(COMPOSITION);

        assertThat(JsonPointers.archetypeNodeChild(composition, "at0004", "value"))
                .contains("/content/0/data/events/0/data/items/0/value");
        assertThat(JsonPointers.archetypeNodeChild(composition, "at0003", "time"))
                .contains("/content/0/data/events/0/time");
        assertThat(JsonPointers.archetypeNode(composition, "openEHR-EHR-OBSERVATION.pulse.v2"))
                .contains("/content/0");
        assertThat(JsonPointers.archetypeNode(composition, "at9999")).isEmpty();
    }

    @Test
    void reportsWhatTheRoundTripDropped() throws Exception {
        var before = mapper.readTree("""
                {"code":{"coding":[{"system":"http://loinc.org","code":"40443-4"}]},
                 "subject":{"reference":"Patient/demo-patient"},
                 "valueQuantity":{"value":58}}
                """);
        var after = mapper.readTree("""
                {"code":{"coding":[{"code":"40443-4"}]},
                 "valueQuantity":{"value":58.0,"unit":"/min"}}
                """);

        assertThat(JsonPointers.diff(before, after))
                .extracting(RoundTripDifference::pointer, RoundTripDifference::kind)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("/code/coding/0/system", "lost"),
                        org.assertj.core.groups.Tuple.tuple("/subject/reference", "lost"),
                        org.assertj.core.groups.Tuple.tuple("/valueQuantity/unit", "added"));
    }

    /** 58 and 58.0 are the same heart rate; reporting that as a change would only be noise. */
    @Test
    void ignoresHowANumberIsSpelled() throws Exception {
        assertThat(JsonPointers.diff(mapper.readTree("{\"v\":58}"), mapper.readTree("{\"v\":58.0}")))
                .isEmpty();
    }
}
