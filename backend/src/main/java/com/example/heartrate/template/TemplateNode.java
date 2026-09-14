package com.example.heartrate.template;

import java.util.List;

/**
 * One node of the operational template: what the model allows at this place.
 *
 * @param rmType the reference model type — COMPOSITION, OBSERVATION, DV_QUANTITY
 * @param attribute which attribute of the parent this is reached through, e.g. {@code events}
 * @param nodeId the archetype node id, {@code at0004}, where there is one
 * @param name the archetype's own word for this node — "Rate", "Any event"
 * @param archetype set where a new archetype begins here
 * @param occurrences {@code 1..1}, {@code 0..*} — what the template permits
 * @param mandatory whether the model insists on it
 * @param path the openEHR path to this node, which is what AQL addresses
 * @param filled whether the stored data actually reaches this node
 */
public record TemplateNode(
        String rmType,
        String attribute,
        String nodeId,
        String name,
        String archetype,
        String occurrences,
        boolean mandatory,
        String path,
        boolean filled,
        List<TemplateNode> children) {

    TemplateNode withChildren(List<TemplateNode> children) {
        return new TemplateNode(
                rmType, attribute, nodeId, name, archetype, occurrences, mandatory, path, filled, children);
    }

    TemplateNode withFilled(boolean filled) {
        return new TemplateNode(
                rmType, attribute, nodeId, name, archetype, occurrences, mandatory, path, filled, children);
    }
}
