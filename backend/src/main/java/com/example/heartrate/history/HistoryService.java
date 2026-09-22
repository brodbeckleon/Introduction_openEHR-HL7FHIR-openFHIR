package com.example.heartrate.history;

import com.example.heartrate.openehr.EhrbaseClient;
import com.example.heartrate.patient.EhrResolver;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Reads back what a day used to say, not just what it says now.
 *
 * <p>openEHR has no overwrite: correcting a reading adds a version and keeps the one before it. That
 * is the most consequential difference between a clinical data repository and an ordinary database,
 * and it is invisible until someone shows the old versions — which is what this exists to do.
 */
@Service
public class HistoryService {

    private static final Logger log = LoggerFactory.getLogger(HistoryService.class);

    private static final String PULSE_EVENT = "o/data[at0002]/events[at0003]";

    /** The compositions covering one day, whichever versions they are currently on. */
    private static final String DAY_AQL = """
            SELECT c/uid/value AS version_uid
            FROM EHR e[ehr_id/value=$ehrId]
              CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.encounter.v1]
                CONTAINS OBSERVATION o[openEHR-EHR-OBSERVATION.pulse.v2]
            WHERE %s/time/value >= $from AND %s/time/value < $to
            """.formatted(PULSE_EVENT, PULSE_EVENT);

    private final EhrbaseClient ehrbase;
    private final EhrResolver ehrResolver;

    public HistoryService(EhrbaseClient ehrbase, EhrResolver ehrResolver) {
        this.ehrbase = ehrbase;
        this.ehrResolver = ehrResolver;
    }

    public DayHistory of(LocalDate day, String patientId) {
        var ehrId = ehrResolver.ehrIdFor(patientId);
        var parameters = Map.<String, Object>of(
                "ehrId", ehrId,
                "from", startOfDay(day),
                "to", startOfDay(day.plusDays(1)));

        List<List<JsonNode>> rows;
        try {
            rows = ehrbase.query(DAY_AQL, parameters);
        } catch (Exception e) {
            log.warn("Could not look up compositions on {}: {}", day, e.getMessage());
            return new DayHistory(day, List.of());
        }

        var histories = rows.stream()
                .filter(row -> !row.isEmpty() && !row.get(0).isNull())
                .map(row -> objectUid(row.get(0).asText()))
                .distinct()
                .map(objectUid -> historyOf(objectUid, ehrId))
                .filter(history -> !history.versions().isEmpty())
                .toList();

        return new DayHistory(day, histories);
    }

    private DayHistory.CompositionHistory historyOf(String objectUid, String ehrId) {
        JsonNode history;
        try {
            history = ehrbase.revisionHistory(ehrId, objectUid);
        } catch (Exception e) {
            log.warn("Could not read the revision history of {}: {}", objectUid, e.getMessage());
            return new DayHistory.CompositionHistory(objectUid, List.of());
        }

        var items = history != null && history.isArray() ? history : history == null ? null : history.path("items");
        if (items == null || !items.isArray()) {
            return new DayHistory.CompositionHistory(objectUid, List.of());
        }

        var versions = new ArrayList<DayHistory.Version>();
        for (JsonNode item : items) {
            var versionUid = item.path("version_id").path("value").asText(null);
            if (versionUid == null) {
                continue;
            }
            var audit = item.path("audits").path(0);
            versions.add(new DayHistory.Version(
                    versionUid,
                    versionNumber(versionUid),
                    audit.path("time_committed").path("value").asText(null),
                    audit.path("change_type").path("value").asText(null),
                    committerOf(audit),
                    bpmAt(objectUid, versionUid, ehrId),
                    false));
        }

        versions.sort(Comparator.comparingInt(DayHistory.Version::version));
        if (!versions.isEmpty()) {
            // The highest version is the one in force; saying so beats making the reader count.
            var last = versions.size() - 1;
            var latest = versions.get(last);
            versions.set(last, new DayHistory.Version(latest.versionUid(), latest.version(),
                    latest.committed(), latest.changeType(), latest.committer(), latest.bpm(), true));
        }
        return new DayHistory.CompositionHistory(objectUid, List.copyOf(versions));
    }

    /** The rate as one specific version recorded it — the point of the whole exercise. */
    private Double bpmAt(String objectUid, String versionUid, String ehrId) {
        try {
            var version = ehrbase.versionAt(ehrId, objectUid, versionUid);
            var magnitude = findRate(version == null ? null : version.path("data"));
            return magnitude == null ? null : magnitude;
        } catch (Exception e) {
            log.warn("Could not read version {}: {}", versionUid, e.getMessage());
            return null;
        }
    }

    /**
     * Finds the Rate element wherever it sits, by its archetype node id rather than by a fixed path,
     * so a changed template moves the value instead of breaking this.
     */
    static Double findRate(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        if (node.isObject()) {
            if ("at0004".equals(node.path("archetype_node_id").asText(null))) {
                var magnitude = node.path("value").path("magnitude");
                if (magnitude.isNumber()) {
                    return magnitude.asDouble();
                }
            }
            var names = node.fieldNames();
            while (names.hasNext()) {
                var found = findRate(node.get(names.next()));
                if (found != null) {
                    return found;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                var found = findRate(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** EHRbase reports the committer as a PARTY_IDENTIFIED; the name is the readable part. */
    private static String committerOf(JsonNode audit) {
        var name = audit.path("committer").path("name").asText(null);
        return name == null || name.isBlank() ? null : name;
    }

    /** {@code uuid::node::3} — the object id is everything before the first marker. */
    static String objectUid(String versionUid) {
        int marker = versionUid.indexOf("::");
        return marker < 0 ? versionUid : versionUid.substring(0, marker);
    }

    static int versionNumber(String versionUid) {
        int marker = versionUid.lastIndexOf("::");
        if (marker < 0) {
            return 1;
        }
        try {
            return Integer.parseInt(versionUid.substring(marker + 2));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** EHRbase compares these bounds as text, so the seconds have to be spelled out. */
    private static String startOfDay(LocalDate day) {
        return day.atStartOfDay().toInstant(ZoneOffset.UTC).toString();
    }
}
