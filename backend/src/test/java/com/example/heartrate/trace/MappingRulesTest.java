package com.example.heartrate.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.example.heartrate.config.HeartrateProperties;
import java.util.List;
import com.example.heartrate.config.TestMessages;
import org.junit.jupiter.api.Test;

class MappingRulesTest {

    private final MappingLibrary library = new MappingLibrary(
            new HeartrateProperties(
                    new HeartrateProperties.Ehrbase("http://localhost", "u", "p"),
                    new HeartrateProperties.OpenFhir("http://localhost"),
                    new HeartrateProperties.FhirStore("http://localhost"),
                    "heartrate_monitor.v1", "max-mustermann", List.of(), "composer", "CH",
                    "../openfhir-bootstrap"),
            TestMessages.create());

    @Test
    void readsTheCorrespondenceARuleDeclares() {
        var rate = library.rules().stream()
                .filter(r -> r.name().equals("rate"))
                .findFirst()
                .orElseThrow();

        assertThat(rate.kind()).isEqualTo("correspondence");
        assertThat(rate.fhir()).isEqualTo("$resource.value");
        assertThat(rate.openehr())
                .isEqualTo("$archetype/data[at0002]/events[at0003]/data[at0001]/items[at0004]");
        assertThat(rate.file()).isEqualTo("pulse.model.yaml");
        assertThat(rate.fromLine()).isPositive();
        assertThat(rate.toLine()).isGreaterThan(rate.fromLine());
    }

    /**
     * A `manual` entry writes a fixed value into outgoing FHIR rather than connecting two paths.
     * That is how an exported Observation gets a LOINC code openEHR never stored, so it is worth
     * listing as its own kind rather than hiding.
     */
    @Test
    void readsTheConstantsAsSuch() {
        assertThat(library.rules())
                .filteredOn(r -> r.kind().equals("constant"))
                .extracting(MappingRule::name, MappingRule::fhir, MappingRule::value)
                .contains(
                        tuple("code", "code.coding.code", "40443-4"),
                        tuple("status", "status", "final"),
                        tuple("category", "category.coding.code", "vital-signs"));
    }

    /** Nesting is meaningful: a nested rule only applies inside its parent. */
    @Test
    void keepsTheNestingOfTheEncounterMapping() {
        assertThat(library.rules())
                .filteredOn(r -> r.file().equals("heartrate-encounter.model.yaml"))
                .extracting(MappingRule::name, MappingRule::depth)
                .containsExactly(
                        tuple("pulseParent", 1), tuple("iteratePulse", 4), tuple("pulseSlot", 7));
    }

    @Test
    void coversEveryMappingFileThatHasRules() {
        assertThat(library.rules()).extracting(MappingRule::file).doesNotContain("heartrate.context.yaml");
        assertThat(library.rules()).hasSizeGreaterThanOrEqualTo(9);
    }
}
