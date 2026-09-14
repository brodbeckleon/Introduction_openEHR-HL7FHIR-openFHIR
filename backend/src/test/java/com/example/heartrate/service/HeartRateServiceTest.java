package com.example.heartrate.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.heartrate.model.DailyRestingHeartRate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class HeartRateServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /** One row as READINGS_AQL selects it: measured_at, bpm, committed. */
    private List<JsonNode> row(String measuredAt, double bpm, String committed) {
        return List.of(mapper.valueToTree(measuredAt), mapper.valueToTree(bpm), mapper.valueToTree(committed));
    }

    /**
     * The bug this guards: a day with several compositions used to resolve to whichever the store
     * happened to return last, so the first value ever written won and every correction vanished.
     */
    @Test
    void keepsTheValueCommittedLast() {
        var series = HeartRateService.newestPerDay(List.of(
                row("2026-09-08T00:00:00Z", 50, "2026-09-11T11:35:13.741054Z"),
                row("2026-09-08T00:00:00Z", 72, "2026-09-14T07:24:09.949200Z"),
                row("2026-09-08T00:00:00Z", 61, "2026-09-14T07:20:22.868999Z")));

        assertThat(series).containsExactly(new DailyRestingHeartRate(LocalDate.of(2026, 9, 8), 72));
    }

    /** Result order says nothing about commit order, so the newest must win from either direction. */
    @Test
    void doesNotDependOnTheOrderTheRowsArriveIn() {
        var rows = List.of(
                row("2026-09-08T00:00:00Z", 44, "2026-09-14T09:00:00Z"),
                row("2026-09-08T00:00:00Z", 88, "2026-09-13T09:00:00Z"));

        assertThat(HeartRateService.newestPerDay(rows))
                .isEqualTo(HeartRateService.newestPerDay(rows.reversed()))
                .containsExactly(new DailyRestingHeartRate(LocalDate.of(2026, 9, 8), 44));
    }

    @Test
    void sortsDaysAndKeepsThemSeparate() {
        var series = HeartRateService.newestPerDay(List.of(
                row("2026-09-10T00:00:00Z", 46, "2026-09-14T09:00:00Z"),
                row("2026-09-08T00:00:00Z", 51, "2026-09-14T09:00:00Z"),
                row("2026-09-09T00:00:00Z", 49, "2026-09-14T09:00:00Z")));

        assertThat(series).extracting(DailyRestingHeartRate::date)
                .containsExactly(
                        LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 10));
    }

    /**
     * EHRbase compares DV_DATE_TIME bounds as text. OffsetDateTime.toString() drops the seconds when
     * they are zero, which is valid ISO-8601 and silently matches nothing — a day window written that
     * way returned no rows at all.
     */
    @Test
    void writesTimestampsWithTheSecondsSpelledOut() {
        assertThat(HeartRateService.startOfDay(LocalDate.of(2026, 9, 8))).isEqualTo("2026-09-08T00:00:00Z");
    }
}
