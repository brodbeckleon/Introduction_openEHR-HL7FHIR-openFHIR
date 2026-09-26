package com.example.heartrate.openfhir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpenFhirLoaderTest {

    private static final Path MAPPINGS = Path.of("../openfhir-bootstrap");

    private final ObjectMapper mapper = new ObjectMapper();

    /** A context is one per template, so the template it names is how its entry is found again. */
    @Test
    void recognisesAContextByItsTemplate() throws IOException {
        var identity = OpenFhirLoader.identify(Files.readString(MAPPINGS.resolve("heartrate.context.yaml")));

        assertThat(identity.kind()).isEqualTo("fc/context");
        assertThat(identity.keyPath()).containsExactly("context", "template", "id");
        assertThat(identity.key()).isEqualTo("heartrate_monitor.v1");
    }

    /** A model by the name in its metadata — the name a context refers to it by. */
    @Test
    void recognisesAModelByItsName() throws IOException {
        var identity = OpenFhirLoader.identify(Files.readString(MAPPINGS.resolve("pulse.model.yaml")));

        assertThat(identity.kind()).isEqualTo("fc/model");
        assertThat(identity.key()).isEqualTo("OBSERVATION.pulse.v2");
    }

    /**
     * A file broken on purpose in the editor never reaches openFHIR as a new entry: it cannot be
     * matched with the one it would replace, so it is refused here and openFHIR keeps its version.
     */
    @Test
    void refusesWhatIsNotAContextOrAModel() {
        assertThatThrownBy(() -> OpenFhirLoader.identify("type: mapping\nmetadata:\n  name: x\n"))
                .hasMessageContaining("not context or model");
        assertThatThrownBy(() -> OpenFhirLoader.identify("type: model\nspec: {}\n"))
                .hasMessageContaining("metadata.name is missing");
    }

    /** openFHIR keeps what it is given across restarts, so a second start has to replace, not add. */
    @Test
    void findsTheEntryAFileReplaces() {
        var models = listing("""
                [{"id": "a1", "metadata": {"name": "openEHR-EHR-COMPOSITION.encounter.v1"}},
                 {"id": "b2", "metadata": {"name": "OBSERVATION.pulse.v2"}}]""");
        var pulse = new OpenFhirLoader.Identity("fc/model", List.of("metadata", "name"), "OBSERVATION.pulse.v2");

        assertThat(OpenFhirLoader.existingId(models, pulse)).contains("b2");
    }

    @Test
    void findsNothingWhenOpenFhirHoldsNoSuchEntry() {
        var templates = listing("""
                [{"id": "t1", "templateId": "some_other_template.v1"}]""");
        var ours = new OpenFhirLoader.Identity("opt", List.of("templateId"), "heartrate_monitor.v1");

        assertThat(OpenFhirLoader.existingId(templates, ours)).isEmpty();
        assertThat(OpenFhirLoader.existingId(null, ours)).isEmpty();
    }

    private JsonNode listing(String json) {
        try {
            return mapper.readTree(json);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
