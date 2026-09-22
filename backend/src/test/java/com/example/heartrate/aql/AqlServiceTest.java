package com.example.heartrate.aql;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.heartrate.config.TestMessages;
import com.example.heartrate.openehr.EhrbaseClient;
import com.example.heartrate.patient.EhrResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientResponseException;

class AqlServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /** Stands in for EHRbase: answers with whatever the test wants, or throws it. */
    private AqlService serviceAnswering(JsonNode answer, RuntimeException failure) {
        var ehrbase = new EhrbaseClient(null, null) {
            @Override
            public JsonNode queryRaw(String aql, Map<String, Object> parameters) {
                if (failure != null) {
                    throw failure;
                }
                return answer;
            }
        };
        // The resolver answers from memory: these tests are about AQL, and routing it through the
        // same stub would make the EHR lookup consume the answer the query is supposed to get.
        var resolver = new EhrResolver(ehrbase) {
            @Override
            public String ehrIdFor(String patientId) {
                return "ehr-id";
            }
        };
        return new AqlService(ehrbase, TestMessages.create(), resolver, mapper);
    }


    @Test
    void refusesAnythingThatIsNotASelect() {
        var service = serviceAnswering(null, null);

        assertThat(service.run("DELETE FROM EHR e", "demo-patient").error()).contains("SELECT");
        assertThat(service.run("   ", "demo-patient").error()).isNotNull();
        assertThat(service.run(null, "demo-patient").error()).isNotNull();
    }

    @Test
    void passesTheColumnMetadataThrough() throws Exception {
        var answer = mapper.readTree("""
                {"columns":[{"path":"o/data[at0002]/events[at0003]/time/value","name":"measured_at"},
                            {"name":"bpm"}],
                 "rows":[["2026-09-11T00:00:00Z", 46.0]]}
                """);

        var result = serviceAnswering(answer, null).run("SELECT x FROM EHR e", "demo-patient");

        assertThat(result.error()).isNull();
        assertThat(result.columns()).containsExactly(
                new AqlResult.Column("o/data[at0002]/events[at0003]/time/value", "measured_at"),
                new AqlResult.Column(null, "bpm"));
        assertThat(result.rows()).hasSize(1);
        assertThat(result.returned()).isEqualTo(1);
        assertThat(result.truncated()).isFalse();
    }

    /**
     * The complaint someone can act on is inside the JSON body EHRbase sends; the exception's own
     * message is the status line wrapped around it.
     */
    @Test
    void reportsWhatEhrbaseObjectedTo() {
        var failure = new RestClientResponseException(
                "400 : \"{...}\"", HttpStatus.BAD_REQUEST, "Bad Request", null,
                """
                {"error":"Bad Request","message":"Could not parse AQL query: mismatched input 'WHERE'"}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                java.nio.charset.StandardCharsets.UTF_8);

        var result = serviceAnswering(null, failure).run("SELECT nonsense FROM WHERE", "demo-patient");

        assertThat(result.error()).isEqualTo("Could not parse AQL query: mismatched input 'WHERE'");
        assertThat(result.error()).doesNotContain("400");
    }

    @Test
    void saysWhenItHeldRowsBack() throws Exception {
        var rows = new StringBuilder("[");
        for (int i = 0; i < 300; i++) {
            rows.append(i > 0 ? "," : "").append("[").append(i).append("]");
        }
        var answer = mapper.readTree("{\"columns\":[{\"name\":\"n\"}],\"rows\":" + rows + "]}");

        var result = serviceAnswering(answer, null).run("SELECT n FROM EHR e", "demo-patient");

        assertThat(result.rows()).hasSize(200);
        assertThat(result.returned()).isEqualTo(300);
        assertThat(result.truncated()).isTrue();
    }

    /** A wrong path is not an error: rows come back with nulls in them, and that has to survive. */
    @Test
    void keepsNullsRatherThanDroppingThem() throws Exception {
        var answer = mapper.readTree("{\"columns\":[{\"name\":\"bpm\"}],\"rows\":[[null],[null]]}");

        var result = serviceAnswering(answer, null).run("SELECT x FROM EHR e", "demo-patient");

        assertThat(result.rows()).hasSize(2);
        assertThat(result.rows().get(0).get(0).isNull()).isTrue();
    }

    @Test
    void acceptsSelectInAnyCase() throws Exception {
        var answer = mapper.readTree("{\"columns\":[],\"rows\":[]}");

        assertThat(serviceAnswering(answer, null).run("select c from EHR e", "demo-patient").error()).isNull();
    }
}
