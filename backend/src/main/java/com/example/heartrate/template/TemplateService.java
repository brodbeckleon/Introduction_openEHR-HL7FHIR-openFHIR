package com.example.heartrate.template;

import com.example.heartrate.config.HeartrateProperties;
import com.example.heartrate.openehr.EhrbaseClient;
import com.example.heartrate.patient.EhrResolver;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * The operational template, and how much of it the data actually uses.
 *
 * <p>A template describes everything a composition <em>may</em> contain. What a system stores is
 * usually a small part of that, and the gap is invisible from either side on its own: the JSON shows
 * only what is there, the template only what could be. Putting them side by side answers a question
 * that otherwise needs reading both — including, here, why the minimum and maximum of a day are not
 * stored even though the template makes room for them.
 */
@Service
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    /** One composition is enough to see which nodes the mapping reaches; they are all alike. */
    private static final String ONE_COMPOSITION_AQL = """
            SELECT c AS composition
            FROM EHR e[ehr_id/value=$ehrId]
              CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.encounter.v1]
            LIMIT 1
            """;

    private final EhrbaseClient ehrbase;
    private final HeartrateProperties properties;
    private final EhrResolver ehrResolver;

    public TemplateService(
            EhrbaseClient ehrbase, HeartrateProperties properties, EhrResolver ehrResolver) {
        this.ehrbase = ehrbase;
        this.properties = properties;
        this.ehrResolver = ehrResolver;
    }

    /**
     * @param templateId the template this describes
     * @param root the tree of what the model allows, each node marked with whether data reaches it
     * @param nodes how many nodes the template defines
     * @param filled how many of them the stored data actually uses
     * @param hasData false when the record is empty, in which case nothing is marked
     */
    public record TemplateView(
            String templateId, TemplateNode root, int nodes, int filled, boolean hasData) {}

    public TemplateView describe(String patientId) {
        TemplateNode root;
        try (var opt = new ClassPathResource("heartrate_monitor.opt").getInputStream()) {
            root = TemplateParser.parse(opt);
        } catch (Exception e) {
            throw new IllegalStateException("Could not read the operational template", e);
        }

        var composition = anyComposition(ehrResolver.ehrIdFor(patientId));
        var marked = composition == null ? root : mark(root, composition);
        return new TemplateView(
                properties.templateId(), marked, count(marked, false), count(marked, true), composition != null);
    }

    /** One stored composition, or null when the record is still empty. */
    private JsonNode anyComposition(String ehrId) {
        try {
            var rows = ehrbase.query(ONE_COMPOSITION_AQL, Map.of("ehrId", ehrId));
            return rows.isEmpty() || rows.get(0).isEmpty() || rows.get(0).get(0).isNull()
                    ? null
                    : rows.get(0).get(0);
        } catch (Exception e) {
            log.warn("Could not read a composition to compare the template against: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Walks template and data together, marking every template node the data reaches.
     *
     * <p>Matching is structural rather than by path: a template child is reached through its
     * {@code rm_attribute_name}, and among the candidates there it is identified by its archetype
     * node id where it has one and by its reference model type where it does not.
     */
    private static TemplateNode mark(TemplateNode node, JsonNode data) {
        var children = new ArrayList<TemplateNode>();
        for (var child : node.children()) {
            var match = matchIn(data, child);
            children.add(match == null ? child.withChildren(markNone(child.children())) : mark(child, match));
        }
        return node.withFilled(true).withChildren(List.copyOf(children));
    }

    private static List<TemplateNode> markNone(List<TemplateNode> children) {
        return children.stream().map(child -> child.withChildren(markNone(child.children()))).toList();
    }

    private static JsonNode matchIn(JsonNode parent, TemplateNode child) {
        if (parent == null || child.attribute() == null) {
            return null;
        }
        var candidates = parent.path(child.attribute());
        if (candidates.isMissingNode() || candidates.isNull()) {
            return null;
        }

        var marker = child.archetype() != null ? child.archetype() : child.nodeId();
        for (JsonNode candidate : candidates.isArray() ? candidates : List.of(candidates)) {
            if (matches(candidate, marker, child.rmType())) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean matches(JsonNode candidate, String marker, String rmType) {
        if (!candidate.isObject()) {
            return false;
        }
        if (marker != null) {
            return marker.equals(candidate.path("archetype_node_id").asText(null));
        }
        var type = candidate.path("_type").asText(null);
        // A composition omits _type where it is implied by the attribute, so an untyped object
        // sitting at the right attribute is taken as the match.
        return type == null || rmType == null || type.equals(rmType);
    }

    private static int count(TemplateNode node, boolean onlyFilled) {
        int self = onlyFilled && !node.filled() ? 0 : 1;
        for (var child : node.children()) {
            self += count(child, onlyFilled);
        }
        return self;
    }
}
