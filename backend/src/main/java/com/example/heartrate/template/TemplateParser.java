package com.example.heartrate.template;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reads the operational template into a tree of what the model allows.
 *
 * <p>The {@code .opt} is the template as a machine reads it: every node the data may have, what type
 * it must be, how often it may occur, and — through the archetype's own term definitions — what a
 * human calls it. Uploaded to EHRbase it is what a composition is validated against; uploaded to
 * openFHIR it is what paths resolve against. It is the third thing the mappings depend on and the
 * only one the application never shows, which is what this fixes.
 */
final class TemplateParser {

    private static final String NS = "http://schemas.openehr.org/v1";

    private TemplateParser() {}

    static TemplateNode parse(InputStream opt) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        // The template is a local file, but a parser that resolves external entities is a hazard
        // wherever it reads from; turning it off costs nothing here.
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setNamespaceAware(true);

        var document = factory.newDocumentBuilder().parse(opt);
        var definition = child(document.getDocumentElement(), "definition")
                .orElseThrow(() -> new IllegalStateException("The template has no definition"));
        return read(definition, null, "", Map.of());
    }

    /**
     * @param terms the term definitions of the archetype in force, which a nested archetype replaces
     */
    private static TemplateNode read(Element node, String attribute, String parentPath, Map<String, String> terms) {
        var rmType = text(node, "rm_type_name");
        var nodeId = text(node, "node_id");
        var archetype = child(node, "archetype_id").flatMap(id -> child(id, "value")).map(Element::getTextContent)
                .map(String::strip)
                .orElse(null);

        // A node that starts a new archetype brings its own vocabulary with it.
        var vocabulary = archetype != null ? termsOf(node) : terms;
        var name = nodeId == null || nodeId.isBlank() ? null : vocabulary.get(nodeId);

        var occurrences = occurrencesOf(node);
        var path = pathOf(parentPath, attribute, nodeId, archetype);

        var children = new ArrayList<TemplateNode>();
        for (var attributes : elements(node, "attributes")) {
            var rmAttribute = text(attributes, "rm_attribute_name");
            for (var candidate : elements(attributes, "children")) {
                children.add(read(candidate, rmAttribute, path, vocabulary));
            }
        }

        return new TemplateNode(
                rmType,
                attribute,
                blankToNull(nodeId),
                name,
                archetype,
                occurrences,
                occurrences != null && !occurrences.startsWith("0"),
                path,
                false,
                List.copyOf(children));
    }

    /**
     * The openEHR path of a node: attributes are named, and archetyped nodes carry their id in
     * brackets. This is the same shape AQL uses, which is the point of showing it.
     */
    private static String pathOf(String parentPath, String attribute, String nodeId, String archetype) {
        if (attribute == null) {
            return "";
        }
        var marker = archetype != null ? archetype : nodeId;
        var segment = marker == null || marker.isBlank()
                ? attribute
                : attribute + "[" + marker + "]";
        return parentPath + "/" + segment;
    }

    /** {@code 1..1}, {@code 0..*} — how often the template permits this node. */
    private static String occurrencesOf(Element node) {
        return child(node, "occurrences").map(occurrences -> {
            var lower = text(occurrences, "lower");
            var upper = "true".equals(text(occurrences, "upper_unbounded"))
                    ? "*"
                    : text(occurrences, "upper");
            return lower == null && upper == null ? null : (lower == null ? "0" : lower) + ".." + (upper == null ? "*" : upper);
        }).orElse(null);
    }

    /** The archetype's own names for its nodes, keyed by {@code at####}. */
    private static Map<String, String> termsOf(Element node) {
        var terms = new HashMap<String, String>();
        for (var definition : elements(node, "term_definitions")) {
            var code = definition.getAttribute("code");
            for (var item : elements(definition, "items")) {
                if ("text".equals(item.getAttribute("id"))) {
                    terms.put(code, item.getTextContent().strip());
                }
            }
        }
        return Map.copyOf(terms);
    }

    private static List<Element> elements(Element parent, String name) {
        var found = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element element
                    && name.equals(element.getLocalName())
                    && (element.getNamespaceURI() == null || NS.equals(element.getNamespaceURI()))) {
                found.add(element);
            }
        }
        return found;
    }

    private static java.util.Optional<Element> child(Element parent, String name) {
        var found = elements(parent, name);
        return found.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(found.get(0));
    }

    private static String text(Element parent, String name) {
        return child(parent, name).map(Element::getTextContent).map(String::strip).orElse(null);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
