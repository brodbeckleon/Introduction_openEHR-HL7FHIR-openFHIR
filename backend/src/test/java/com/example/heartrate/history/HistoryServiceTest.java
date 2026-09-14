package com.example.heartrate.history;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class HistoryServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void readsTheObjectIdAndVersionOutOfAVersionUid() {
        var uid = "fb8677bc-972a-4a62-82e8-4d19b8835405::heartrate.monitor.local::5";

        assertThat(HistoryService.objectUid(uid)).isEqualTo("fb8677bc-972a-4a62-82e8-4d19b8835405");
        assertThat(HistoryService.versionNumber(uid)).isEqualTo(5);
    }

    /** A uid without the suffix is the object itself, which is version 1 as far as this is concerned. */
    @Test
    void copesWithAUidThatCarriesNoVersion() {
        assertThat(HistoryService.objectUid("fb8677bc")).isEqualTo("fb8677bc");
        assertThat(HistoryService.versionNumber("fb8677bc")).isEqualTo(1);
    }

    /** The rate is found by archetype node id, so a differently shaped composition still works. */
    @Test
    void findsTheRateWhereverItSits() throws Exception {
        var composition = mapper.readTree("""
                {"content":[{"data":{"events":[{"data":{"items":[
                  {"name":{"value":"Rate"},
                   "value":{"_type":"DV_QUANTITY","magnitude":46.0,"units":"/min"},
                   "archetype_node_id":"at0004"}]}}]}}]}
                """);

        assertThat(HistoryService.findRate(composition)).isEqualTo(46.0);
    }

    @Test
    void answersNullWhenThereIsNoRate() throws Exception {
        assertThat(HistoryService.findRate(mapper.readTree("""
                {"content":[{"data":{"items":[{"archetype_node_id":"at0099","value":{"magnitude":7}}]}}]}
                """)))
                .isNull();
        assertThat(HistoryService.findRate(null)).isNull();
    }
}
