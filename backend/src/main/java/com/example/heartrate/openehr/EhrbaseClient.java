package com.example.heartrate.openehr;

import com.example.heartrate.config.HeartrateProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Thin wrapper over the openEHR REST API as implemented by EHRbase.
 */
@Component
public class EhrbaseClient {

    private static final Logger log = LoggerFactory.getLogger(EhrbaseClient.class);
    private static final String API = "/rest/openehr/v1";

    /** Which EHR belongs to a patient. The answer lives in openEHR itself, not in this service. */
    public static final String EHR_BY_SUBJECT_AQL = """
            SELECT e/ehr_id/value AS ehr_id
            FROM EHR e
            WHERE e/ehr_status/subject/external_ref/id/value = $patientId
            """;

    private final RestClient client;
    private final HeartrateProperties properties;

    public EhrbaseClient(@Qualifier("ehrbaseRestClient") RestClient client, HeartrateProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /**
     * Uploads the operational template. EHRbase answers 409 when it is already known, which is the
     * normal case on every restart after the first.
     */
    public void uploadTemplate(String operationalTemplateXml) {
        try {
            client.post()
                    .uri(API + "/definition/template/adl1.4")
                    .contentType(MediaType.APPLICATION_XML)
                    .body(operationalTemplateXml)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Uploaded operational template {}", properties.templateId());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.info("Operational template {} already present", properties.templateId());
            } else {
                throw e;
            }
        }
    }

    /**
     * The EHR belonging to a patient, if openEHR already holds one.
     *
     * <p>This is the whole bridge between the two worlds, and it is openEHR's own: an EHR carries an
     * {@code EHR_STATUS} whose {@code subject} names who the record is about. Nothing outside EHRbase
     * has to remember which EHR belongs to whom — the question is answerable in AQL.
     */
    public Optional<String> findEhrByPatient(String patientId) {
        var rows = query(EHR_BY_SUBJECT_AQL, Map.of("patientId", patientId));
        return rows.stream()
                .filter(row -> !row.isEmpty() && !row.get(0).isNull())
                .map(row -> row.get(0).asText())
                .findFirst();
    }

    /**
     * Creates an EHR for a patient and answers with its id.
     *
     * <p>The patient id goes into {@code EHR_STATUS.subject} rather than into this service's own
     * configuration, which is what makes {@link #findEhrByPatient} possible at all.
     */
    public String createEhr(String ehrId, String patientId) {
        var ehrStatus = """
                {
                  "_type": "EHR_STATUS",
                  "name": {"_type": "DV_TEXT", "value": "EHR Status"},
                  "archetype_node_id": "openEHR-EHR-EHR_STATUS.generic.v1",
                  "subject": {
                    "_type": "PARTY_SELF",
                    "external_ref": {
                      "_type": "PARTY_REF",
                      "namespace": "heartrate-monitor",
                      "type": "PERSON",
                      "id": {"_type": "GENERIC_ID", "value": "%s", "scheme": "heartrate-monitor"}
                    }
                  },
                  "is_queryable": true,
                  "is_modifiable": true
                }
                """.formatted(patientId);
        try {
            client.put()
                    .uri(API + "/ehr/{ehrId}", ehrId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Prefer", "return=minimal")
                    .body(ehrStatus)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Created EHR {} for patient {}", ehrId, patientId);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.info("EHR {} already exists", ehrId);
            } else {
                throw e;
            }
        }
        return ehrId;
    }

    /** Stores a canonical COMPOSITION and returns the version uid EHRbase assigned. */
    public String createComposition(String ehrId, JsonNode canonicalComposition) {
        var response = client.post()
                .uri(API + "/ehr/{ehrId}/composition", ehrId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Prefer", "return=minimal")
                .body(canonicalComposition)
                .retrieve()
                .toBodilessEntity();
        var location = response.getHeaders().getFirst("ETag");
        return location == null ? null : location.replace("\"", "");
    }

    /**
     * Corrects a stored composition.
     *
     * <p>openEHR has no overwrite: this adds a new version and keeps the old one, which is why the
     * answer carries {@code ::2} where the first version carried {@code ::1}. {@code If-Match} names
     * the version being replaced, so two people correcting the same record cannot silently clobber
     * each other.
     *
     * @param versionedObjectUid the uid without the {@code ::node::version} suffix
     * @param precedingVersionUid the full uid of the version being replaced
     * @return the uid of the version this created
     */
    public String updateComposition(
            String ehrId, String versionedObjectUid, String precedingVersionUid, JsonNode composition) {
        var response = client.put()
                .uri(API + "/ehr/{ehrId}/composition/{uid}", ehrId, versionedObjectUid)
                .contentType(MediaType.APPLICATION_JSON)
                .header("If-Match", precedingVersionUid)
                .header("Prefer", "return=minimal")
                .body(composition)
                .retrieve()
                .toBodilessEntity();
        var etag = response.getHeaders().getFirst("ETag");
        return etag == null ? null : etag.replace("\"", "");
    }

    /**
     * The revision history of one versioned composition: every version, with who committed it, when,
     * and whether it was a creation or a modification.
     *
     * <p>This is not a feature the repository adds on top — an openEHR composition is a
     * {@code VERSIONED_OBJECT} and the history is what it is made of.
     */
    public JsonNode revisionHistory(String ehrId, String versionedObjectUid) {
        return client.get()
                .uri(API + "/ehr/{ehrId}/versioned_composition/{uid}/revision_history", ehrId, versionedObjectUid)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
    }

    /**
     * One specific version, identified by its full {@code uuid::node::version} uid.
     *
     * <p>The uid is a path segment, as the openEHR REST specification defines it. Passing it as a
     * {@code ?version_uid=} query parameter instead is accepted by EHRbase and quietly answers with
     * the latest version, which looks like a record whose history never changed.
     */
    public JsonNode versionAt(String ehrId, String versionedObjectUid, String versionUid) {
        return client.get()
                .uri(API + "/ehr/{ehrId}/versioned_composition/{uid}/version/{versionUid}",
                        ehrId, versionedObjectUid, versionUid)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
    }

    /** Reads one stored composition back in canonical form. */
    public JsonNode getComposition(String ehrId, String versionedObjectUid) {
        return client.get()
                .uri(API + "/ehr/{ehrId}/composition/{uid}", ehrId, versionedObjectUid)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode.class);
    }

    /**
     * Runs an AQL query and answers with the whole openEHR result set, including the column
     * metadata. The playground shows that metadata; everything else only wants the rows.
     */
    public JsonNode queryRaw(String aql, Map<String, Object> parameters) {
        var body = Map.of("q", aql, "query_parameters", parameters);
        var result = client.post()
                .uri(API + "/query/aql")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return result == null ? com.fasterxml.jackson.databind.node.NullNode.getInstance() : result;
    }

    /**
     * Runs an AQL query.
     *
     * @return the {@code rows} array of the openEHR result set
     */
    public List<List<JsonNode>> query(String aql, Map<String, Object> parameters) {
        var result = queryRaw(aql, parameters);

        var rows = result.get("rows");
        if (rows == null || !rows.isArray()) {
            return List.of();
        }
        var result_rows = new java.util.ArrayList<List<JsonNode>>();
        for (JsonNode row : rows) {
            var cells = new java.util.ArrayList<JsonNode>();
            row.forEach(cells::add);
            result_rows.add(cells);
        }
        return result_rows;
    }
}
