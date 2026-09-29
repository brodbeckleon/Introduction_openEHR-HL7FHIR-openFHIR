package com.example.heartrate.template;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TemplateParserTest {

    private TemplateNode parse() throws Exception {
        try (var opt = Files.newInputStream(Path.of("../openfhir-bootstrap/heartrate_monitor.opt"))) {
            return TemplateParser.parse(opt);
        }
    }

    /**
     * Finds a node anywhere in the tree by node id and type.
     *
     * <p>The type is not optional: a node id is unique only within its archetype, and this template
     * has an at0002 in the encounter archetype and another in the pulse one.
     */
    private static Optional<TemplateNode> find(TemplateNode node, String nodeId, String rmType) {
        if (nodeId.equals(node.nodeId()) && rmType.equals(node.rmType())) {
            return Optional.of(node);
        }
        return node.children().stream()
                .map(child -> find(child, nodeId, rmType))
                .flatMap(Optional::stream)
                .findFirst();
    }

    @Test
    void readsTheCompositionAtTheRoot() throws Exception {
        var root = parse();

        assertThat(root.rmType()).isEqualTo("COMPOSITION");
        assertThat(root.archetype()).isEqualTo("openEHR-EHR-COMPOSITION.encounter.v1");
        assertThat(root.children()).isNotEmpty();
    }

    /**
     * The node ids are language-independent; the readable name comes from the archetype's own term
     * definitions, and a nested archetype brings its own set.
     */
    @Test
    void namesNodesFromTheArchetypeTheyBelongTo() throws Exception {
        var rate = find(parse(), "at0004", "ELEMENT").orElseThrow();

        assertThat(rate.name()).isEqualTo("Rate");
        assertThat(rate.rmType()).isEqualTo("ELEMENT");
    }

    /** The path is what AQL addresses, so it has to come out in AQL's own shape. */
    @Test
    void buildsThePathAqlWouldUse() throws Exception {
        var rate = find(parse(), "at0004", "ELEMENT").orElseThrow();

        assertThat(rate.path())
                .isEqualTo("/content[openEHR-EHR-OBSERVATION.pulse.v2]"
                        + "/data[at0002]/events[at0003]/data[at0001]/items[at0004]");
    }

    @Test
    void readsWhatTheTemplatePermits() throws Exception {
        var root = parse();

        var rate = find(root, "at0004", "ELEMENT").orElseThrow();
        assertThat(rate.occurrences()).isEqualTo("0..1");
        assertThat(rate.mandatory()).as("the template allows a pulse with no rate").isFalse();

        var history = find(root, "at0002", "HISTORY").orElseThrow();
        assertThat(history.occurrences()).isEqualTo("1..1");
        assertThat(history.mandatory()).as("an OBSERVATION must have its history").isTrue();
    }

    /**
     * The point of the explorer: the template has a slot for the minimum, maximum and mean of a
     * period that the mapping never fills. If this stops being true the explorer's closing note is
     * wrong and should be rewritten.
     */
    @Test
    void includesTheIntervalEventTheMappingNeverFills() throws Exception {
        var interval = find(parse(), "at1036", "INTERVAL_EVENT").orElseThrow();

        assertThat(interval.rmType()).isEqualTo("INTERVAL_EVENT");
        assertThat(interval.children())
                .extracting(TemplateNode::attribute)
                .contains("math_function");
    }

    @Test
    void marksNothingBeforeDataIsCompared() throws Exception {
        assertThat(allNodes(parse())).allSatisfy(node -> assertThat(node.filled()).isFalse());
    }

    private static List<TemplateNode> allNodes(TemplateNode node) {
        var all = new java.util.ArrayList<TemplateNode>();
        all.add(node);
        node.children().forEach(child -> all.addAll(allNodes(child)));
        return all;
    }
}
