package com.example.heartrate.history;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the record holds about one day, including what it used to hold.
 *
 * <p>Normally a day is one composition with one or more versions. A day can carry several
 * compositions when readings were stored before this service corrected by versioning; those are
 * reported rather than hidden, because pretending otherwise would misrepresent the record.
 */
public record DayHistory(LocalDate date, List<CompositionHistory> compositions) {

    /** One versioned composition and every version of it, oldest first. */
    public record CompositionHistory(String uid, List<Version> versions) {}

    /**
     * @param version the version number, counting from 1
     * @param changeType openEHR's own word for what happened: {@code creation}, {@code modification},
     *     {@code amendment}, {@code deleted}
     * @param bpm the resting heart rate as this version recorded it, or null if it cannot be read
     * @param current whether this is the version in force now
     */
    public record Version(
            String versionUid,
            int version,
            String committed,
            String changeType,
            String committer,
            Double bpm,
            boolean current) {}
}
