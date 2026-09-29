package com.example.heartrate.trace;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.heartrate.config.HeartrateProperties;
import java.util.List;
import com.example.heartrate.config.TestMessages;
import org.junit.jupiter.api.Test;

class MappingLibraryTest {

    private final MappingLibrary library = new MappingLibrary(new HeartrateProperties(
            new HeartrateProperties.Ehrbase("http://localhost", "u", "p"),
            new HeartrateProperties.OpenFhir("http://localhost"),
            "heartrate_monitor.v1",
            "max-mustermann",
            List.of(),
            "composer",
            "CH",
            "../openfhir-bootstrap"),
            TestMessages.create());

    @Test
    void servesEveryMappingFile() {
        assertThat(library.sources())
                .extracting(MappingSource::file)
                .containsExactly(
                        "heartrate.context.yaml",
                        "heartrate-encounter.model.yaml",
                        "pulse.model.yaml");
        assertThat(library.sources()).allSatisfy(source ->
                assertThat(source.content()).contains("grammar: FHIRConnect"));
    }

    /**
     * One template, beside the mappings. Both servers are handed this file, so there is no second
     * copy that could say something else.
     */
    @Test
    void readsTheOneOperationalTemplate() {
        assertThat(library.operationalTemplate())
                .contains("<template_id>")
                .contains("heartrate_monitor.v1");
    }

    /** What openFHIR is handed, and nothing the editor does not know. */
    @Test
    void listsOnlyItsOwnMappings() {
        assertThat(library.files())
                .containsExactly("heartrate.context.yaml", "heartrate-encounter.model.yaml", "pulse.model.yaml");
        assertThat(library.content("../docker-compose.yml")).isEmpty();
    }

    /** The line range is what the UI highlights, so it has to stop at the next rule. */
    @Test
    void findsTheBlockOfOneRule() {
        var rate = library.block("pulse.model.yaml", "- name: \"rate\"").orElseThrow();
        var lines = lines("pulse.model.yaml", rate);

        assertThat(lines).contains("- name: \"rate\"").contains("items[at0004]");
        assertThat(lines).doesNotContain("- name: \"time\"");
    }

    /** A top-level key nests everything below it; the next top-level key ends it. */
    @Test
    void findsTheBlockOfATopLevelKey() {
        var preprocessor = library.block("pulse.model.yaml", "preprocessor:").orElseThrow();
        var lines = lines("pulse.model.yaml", preprocessor);

        assertThat(lines).contains("40443-4").contains("vital-signs");
        assertThat(lines).doesNotContain("mappings:");
    }

    @Test
    void answersEmptyForAnAnchorThatIsNotThere() {
        assertThat(library.block("pulse.model.yaml", "- name: \"nothing\"")).isEmpty();
    }

    private String lines(String file, int[] range) {
        var all = library.sources().stream()
                .filter(source -> source.file().equals(file))
                .findFirst()
                .orElseThrow()
                .content()
                .split("\n", -1);
        var selected = new StringBuilder();
        for (int line = range[0]; line <= range[1]; line++) {
            selected.append(all[line - 1]).append('\n');
        }
        return selected.toString();
    }
}
