package com.example.heartrate.fhir;

import com.example.heartrate.model.Reading;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Makes up a month of plausible resting heart rates, ending today.
 *
 * <p>A sample file checked into the repository would be wrong within a day: the chart shows the last
 * 30 days, so fixed dates walk out of the window and the file has to be regenerated to stay useful.
 * Generating on request removes the problem rather than scheduling it.
 *
 * <p>The values are deterministic for a given end date, so asking twice on the same day gives the
 * same month — a sample that changed under you while you were looking at it would be worse than a
 * stale one.
 */
@Component
public class SampleReadings {

    /** A resting heart rate sits here for most people, and the demo should look like a person. */
    private static final double BASELINE = 54;

    /** Not every day has a reading; the gaps are what the coverage figure is about. */
    private static final int MISSING_IN = 8;

    public List<Reading> lastDays(int days) {
        var today = LocalDate.now(ZoneOffset.UTC);
        var random = new Random(today.toEpochDay());

        var readings = new ArrayList<Reading>();
        for (int back = days - 1; back >= 0; back--) {
            if (random.nextInt(MISSING_IN) == 0) {
                continue;
            }
            var day = today.minusDays(back);
            // A slow wave plus a little noise: a trend to look at rather than a flat line.
            double drift = 4 * Math.sin(back / 5.0);
            double noise = random.nextGaussian() * 1.8;
            double bpm = Math.round(BASELINE + drift + noise);

            readings.add(new Reading(day.atStartOfDay().atOffset(ZoneOffset.UTC), bpm));
        }
        return List.copyOf(readings);
    }
}
