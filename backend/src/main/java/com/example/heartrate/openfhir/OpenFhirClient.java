package com.example.heartrate.openfhir;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls the openFHIR engine, which executes the FHIR Connect mappings in {@code openfhir-bootstrap/}.
 * No clinical data is stored there: it only translates between the two representations.
 */
@Component
public class OpenFhirClient {

    private static final MediaType FHIR_JSON = MediaType.valueOf("application/fhir+json");
    private static final MediaType OPENEHR_JSON = MediaType.valueOf("application/openehr+json");

    private final RestClient client;

    public OpenFhirClient(@Qualifier("openFhirRestClient") RestClient client) {
        this.client = client;
    }

    /** FHIR Bundle/Resource -> canonical openEHR COMPOSITION. */
    public JsonNode toOpenEhr(String fhirJson, String templateId) {
        return client.post()
                .uri(uriBuilder -> uriBuilder.path("/openfhir/toopenehr")
                        .queryParam("templateId", templateId)
                        .queryParam("format", "canonical")
                        .build())
                .contentType(FHIR_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(fhirJson)
                .retrieve()
                .body(JsonNode.class);
    }

    /**
     * Makes openFHIR re-read its bootstrap directory — the operational template and the FHIR Connect
     * mappings. This is why a mapping change needs no rebuild and no restart.
     */
    public JsonNode bootstrap() {
        return client.post()
                .uri("/$bootstrap")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
    }

    /** Canonical openEHR COMPOSITION -> FHIR. */
    public JsonNode toFhir(String canonicalCompositionJson, String templateId) {
        return client.post()
                .uri(uriBuilder -> uriBuilder.path("/openfhir/tofhir")
                        .queryParam("templateId", templateId)
                        .build())
                .contentType(OPENEHR_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(canonicalCompositionJson)
                .retrieve()
                .body(JsonNode.class);
    }
}
