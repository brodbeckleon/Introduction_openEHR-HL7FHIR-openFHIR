package com.example.heartrate.openfhir;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls the openFHIR engine, which executes the FHIR Connect mappings this service hands it.
 * No clinical data is stored there: it only translates between the two representations.
 */
@Component
public class OpenFhirClient {

    private static final MediaType FHIR_JSON = MediaType.valueOf("application/fhir+json");
    private static final MediaType OPENEHR_JSON = MediaType.valueOf("application/openehr+json");
    private static final MediaType YAML = MediaType.valueOf("application/x-yaml");

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
     * Everything openFHIR holds of one kind.
     *
     * @param kind {@code opt} for operational templates, {@code fc/context} or {@code fc/model} for
     *     the two kinds of FHIR Connect mapping
     */
    public JsonNode list(String kind) {
        return client.get()
                .uri("/" + kind)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
    }

    /** Hands openFHIR something it does not have yet. */
    public void create(String kind, String content) {
        client.post()
                .uri("/" + kind)
                .contentType(typeOf(kind))
                .body(content)
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * Replaces something openFHIR already holds, under the id openFHIR gave it.
     *
     * <p>This, not a restart, is why a mapping change needs no rebuild: openFHIR maps with the new
     * version from the next request on. One it refuses leaves the previous version in place.
     */
    public void replace(String kind, String id, String content) {
        client.put()
                .uri("/" + kind + "/{id}", id)
                .contentType(typeOf(kind))
                .body(content)
                .retrieve()
                .toBodilessEntity();
    }

    private static MediaType typeOf(String kind) {
        return kind.equals("opt") ? MediaType.APPLICATION_XML : YAML;
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
