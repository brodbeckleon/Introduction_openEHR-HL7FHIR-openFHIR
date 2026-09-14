package com.example.heartrate.fhir;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.heartrate.model.Reading;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class SampleReadingsTest {

    private final SampleReadings sample = new SampleReadings();

    /** The reason this is generated at all: a checked-in file walks out of the chart's window. */
    @Test
    void endsToday() {
        var readings = sample.lastDays(30);
        var today = LocalDate.now(ZoneOffset.UTC);

        assertThat(readings).isNotEmpty();
        assertThat(days(readings)).allSatisfy(day -> {
            assertThat(day).isAfterOrEqualTo(today.minusDays(29));
            assertThat(day).isBeforeOrEqualTo(today);
        });
        assertThat(days(readings)).doesNotHaveDuplicates().isSorted();
    }

    /** Some days are deliberately missing, which is what the coverage figure counts. */
    @Test
    void leavesGapsButFillsMostOfTheMonth() {
        var readings = sample.lastDays(30);

        assertThat(readings.size()).isBetween(20, 29);
    }

    @Test
    void staysInAPlausibleRange() {
        assertThat(sample.lastDays(30))
                .allSatisfy(reading -> assertThat(reading.beatsPerMinute()).isBetween(35.0, 80.0));
    }

    /** Asking twice on the same day has to give the same month, or the sample shifts as you watch. */
    @Test
    void isTheSameEveryTimeOnAGivenDay() {
        assertThat(sample.lastDays(30)).isEqualTo(sample.lastDays(30));
    }

    private static java.util.List<LocalDate> days(java.util.List<Reading> readings) {
        return readings.stream()
                .map(reading -> reading.measuredAt().atZoneSameInstant(ZoneOffset.UTC).toLocalDate())
                .toList();
    }
}
