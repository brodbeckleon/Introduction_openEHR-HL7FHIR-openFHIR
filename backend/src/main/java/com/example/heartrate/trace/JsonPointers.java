package com.example.heartrate.trace;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Finds and compares places in a JSON document by RFC 6901 pointer.
 *
 * <p>The pointers are computed against what openFHIR actually produced rather than hardcoded, so a
 * changed mapping moves the highlight instead of breaking it.
 */
final class JsonPointers {

    private JsonPointers() {}

    /** The pointer of the first object carrying {@code archetype_node_id: <nodeId>}. */
    static Optional<String> archetypeNode(JsonNode root, String nodeId) {
        return find(root, node -> nodeId.equals(node.path("archetype_node_id").asText(null)));
    }

    /** As {@link #archetypeNode}, with a child appended when the node was found. */
    static Optional<String> archetypeNodeChild(JsonNode root, String nodeId, String child) {
        return archetypeNode(root, nodeId).map(pointer -> pointer + "/" + escape(child));
    }

    static Optional<String> find(JsonNode root, Predicate<JsonNode> match) {
        return root == null ? Optional.empty() : find(root, "", match);
    }

    private static Optional<String> find(JsonNode node, String pointer, Predicate<JsonNode> match) {
        if (node.isObject()) {
            if (match.test(node)) {
                return Optional.of(pointer);
            }
            var names = node.fieldNames();
            while (names.hasNext()) {
                var name = names.next();
                var found = find(node.get(name), pointer + "/" + escape(name), match);
                if (found.isPresent()) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                var found = find(node.get(i), pointer + "/" + i, match);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Compares two documents leaf by leaf.
     *
     * <p>Ids are skipped: openFHIR mints its own on the way back, and a changed uuid says nothing
     * about the fidelity of the mapping.
     */
    static List<RoundTripDifference> diff(JsonNode before, JsonNode after) {
        var left = leaves(before);
        var right = leaves(after);

        var differences = new ArrayList<RoundTripDifference>();
        left.forEach((pointer, value) -> {
            if (isNoise(pointer)) {
                return;
            }
            var other = right.get(pointer);
            if (other == null) {
                differences.add(new RoundTripDifference(pointer, "lost", value, null));
            } else if (!other.equals(value)) {
                differences.add(new RoundTripDifference(pointer, "changed", value, other));
            }
        });
        right.forEach((pointer, value) -> {
            if (!isNoise(pointer) && !left.containsKey(pointer)) {
                differences.add(new RoundTripDifference(pointer, "added", null, value));
            }
        });
        return List.copyOf(differences);
    }

    private static boolean isNoise(String pointer) {
        return pointer.endsWith("/id") || pointer.endsWith("/fullUrl");
    }

    private static Map<String, String> leaves(JsonNode root) {
        var out = new LinkedHashMap<String, String>();
        if (root != null) {
            leaves(root, "", out);
        }
        return out;
    }

    private static void leaves(JsonNode node, String pointer, Map<String, String> out) {
        if (node.isObject()) {
            var names = node.fieldNames();
            while (names.hasNext()) {
                var name = names.next();
                leaves(node.get(name), pointer + "/" + escape(name), out);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                leaves(node.get(i), pointer + "/" + i, out);
            }
        } else {
            out.put(pointer, text(node));
        }
    }

    /** 58 and 58.0 are the same heart rate; comparing their spellings would only produce noise. */
    private static String text(JsonNode node) {
        if (node.isNumber()) {
            return node.decimalValue().stripTrailingZeros().toPlainString();
        }
        return node.isNull() ? "null" : node.asText();
    }

    private static String escape(String name) {
        return name.replace("~", "~0").replace("/", "~1");
    }
}
