package com.example.heartrate.openfhir;

import com.example.heartrate.config.HeartrateProperties;
import com.example.heartrate.trace.MappingLibrary;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.yaml.snakeyaml.Yaml;

/**
 * Hands openFHIR the operational template and the FHIR Connect mappings, from the one copy this
 * service keeps in {@code openfhir-bootstrap/}.
 *
 * <p>openFHIR could read that directory itself, and used to. Handing the files over through its REST
 * API instead makes this service their only reader: the template EHRbase validates against and the
 * template openFHIR resolves paths against are the same bytes rather than two copies that can drift,
 * and every upload shows in the traffic console as the call it is.
 *
 * <p>Every upload is an upsert. openFHIR keeps what it is given in MongoDB across restarts, so each
 * file is matched with what openFHIR already holds — a template by its template id, a context by the
 * template it serves, a model by its name — and replaces it rather than being added a second time.
 */
@Component
public class OpenFhirLoader {

    private static final Logger log = LoggerFactory.getLogger(OpenFhirLoader.class);

    /**
     * What one upload did.
     *
     * @param detail openFHIR's objection when it refused the file, which leaves the version it
     *     loaded before in place
     */
    public record Outcome(String file, boolean applied, String detail) {}

    /**
     * Where a file goes in openFHIR, and how to recognise the entry it replaces.
     *
     * @param kind the collection: {@code opt}, {@code fc/context} or {@code fc/model}
     * @param keyPath the field of a listed entry that identifies it
     * @param key the value that field has for this file
     */
    record Identity(String kind, List<String> keyPath, String key) {}

    private final OpenFhirClient openFhir;
    private final MappingLibrary mappings;
    private final HeartrateProperties properties;

    public OpenFhirLoader(OpenFhirClient openFhir, MappingLibrary mappings, HeartrateProperties properties) {
        this.openFhir = openFhir;
        this.mappings = mappings;
        this.properties = properties;
    }

    /**
     * The template, then every mapping — the order openFHIR needs them in, since a context names a
     * template that has to exist.
     *
     * <p>A file openFHIR refuses is an outcome, not an exception. openFHIR being unreachable is an
     * exception, so the caller can wait for it to come up and try again.
     */
    public List<Outcome> loadAll() {
        var outcomes = new ArrayList<Outcome>();
        outcomes.add(upload(MappingLibrary.TEMPLATE_FILE, mappings.operationalTemplate(),
                new Identity("opt", List.of("templateId"), properties.templateId())));
        mappings.files().forEach(file -> outcomes.add(load(file)));
        return outcomes;
    }

    /** One mapping, as it is on disk now — what the editor calls after writing it. */
    public Outcome load(String file) {
        var content = mappings.content(file)
                .orElseThrow(() -> new IllegalArgumentException("No mapping called " + file));
        Identity identity;
        try {
            identity = identify(content);
        } catch (RuntimeException e) {
            return new Outcome(file, false, "this is not a FHIR Connect context or model ("
                    + e.getMessage() + "), so it was not sent");
        }
        return upload(file, content, identity);
    }

    private Outcome upload(String file, String content, Identity identity) {
        try {
            var existing = existingId(openFhir.list(identity.kind()), identity);
            if (existing.isPresent()) {
                openFhir.replace(identity.kind(), existing.get(), content);
            } else {
                openFhir.create(identity.kind(), content);
            }
            log.info("openFHIR: {} {} as /{}", existing.isPresent() ? "replaced" : "created", file,
                    identity.kind());
            return new Outcome(file, true, null);
        } catch (RestClientResponseException e) {
            var body = e.getResponseBodyAsString();
            log.warn("openFHIR refused {}: {}", file, body.isBlank() ? e.getMessage() : body);
            return new Outcome(file, false, body.isBlank() ? e.getMessage() : body);
        }
    }

    /**
     * Where a FHIR Connect file belongs, read from the file itself.
     *
     * <p>A context is one per template, so the template it names identifies it; a model is found by
     * the name in its metadata, which is also how a context refers to it.
     */
    static Identity identify(String yaml) {
        Map<String, Object> root = new Yaml().load(yaml);
        if (root == null) {
            throw new IllegalArgumentException("the file is empty");
        }
        var type = String.valueOf(root.get("type"));
        return switch (type) {
            case "context" -> new Identity("fc/context", List.of("context", "template", "id"),
                    required(root, "context", "template", "id"));
            case "model" -> new Identity("fc/model", List.of("metadata", "name"),
                    required(root, "metadata", "name"));
            default -> throw new IllegalArgumentException("its type is \"" + type
                    + "\", not context or model");
        };
    }

    /** The id openFHIR gave the entry this file replaces, if it holds one. */
    static Optional<String> existingId(JsonNode listing, Identity identity) {
        if (listing == null) {
            return Optional.empty();
        }
        for (var entry : listing) {
            var value = entry;
            for (var field : identity.keyPath()) {
                value = value.path(field);
            }
            if (identity.key().equals(value.asText(null))) {
                return Optional.of(entry.path("id").asText());
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static String required(Map<String, Object> root, String... path) {
        Object value = root;
        for (var field : path) {
            if (!(value instanceof Map<?, ?> map) || map.get(field) == null) {
                throw new IllegalArgumentException(String.join(".", path) + " is missing");
            }
            value = ((Map<String, Object>) map).get(field);
        }
        return String.valueOf(value);
    }
}
