package com.example.heartrate.service;

import com.example.heartrate.config.HeartrateProperties;
import com.example.heartrate.fhir.HeartRateExtractor;
import com.example.heartrate.fhir.PulseObservations;
import com.example.heartrate.model.DailyRestingHeartRate;
import com.example.heartrate.model.HeartRateSeries;
import com.example.heartrate.model.Reading;
import com.example.heartrate.openehr.EhrbaseClient;
import com.example.heartrate.openfhir.OpenFhirClient;
import com.example.heartrate.patient.EhrResolver;
import com.example.heartrate.patient.PatientDirectory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * The one place where the three standards meet.
 *
 * <p>Writing: a resting heart rate becomes an HL7 FHIR Observation, openFHIR maps it onto the
 * openEHR composition, EHRbase persists it. Reading: AQL pulls the values back out. Exporting: the
 * stored compositions go back through openFHIR, so outgoing FHIR is produced by the same mapping
 * definition as incoming FHIR.
 */
@Service
public class HeartRateService {

    private static final Logger log = LoggerFactory.getLogger(HeartRateService.class);

    private static final String PULSE_EVENT = "o/data[at0002]/events[at0003]";
    private static final String PULSE_RATE = PULSE_EVENT + "/data[at0001]/items[at0004]/value";

    /**
     * The commit time comes along because a day can carry more than one composition — a correction,
     * or a record imported twice before updates existed — and the chart has to show the one that was
     * written last rather than whichever the store happens to return first. EHRbase will not ORDER BY
     * a VERSION path, so the ordering by commit time happens in {@link #series}.
     */
    private static final String READINGS_AQL = """
            SELECT %s/time/value AS measured_at, %s/magnitude AS bpm,
                   v/commit_audit/time_committed/value AS committed
            FROM EHR e[ehr_id/value=$ehrId]
              CONTAINS VERSION v
                CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.encounter.v1]
                  CONTAINS OBSERVATION o[openEHR-EHR-OBSERVATION.pulse.v2]
            WHERE %s/time/value >= $from
            ORDER BY %s/time/value ASC
            """.formatted(PULSE_EVENT, PULSE_RATE, PULSE_EVENT, PULSE_EVENT);

    /** The compositions already recorded for one day, so a new reading corrects rather than piles up. */
    private static final String SAME_DAY_AQL = """
            SELECT c/uid/value AS version_uid, v/commit_audit/time_committed/value AS committed
            FROM EHR e[ehr_id/value=$ehrId]
              CONTAINS VERSION v
                CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.encounter.v1]
                  CONTAINS OBSERVATION o[openEHR-EHR-OBSERVATION.pulse.v2]
            WHERE %s/time/value >= $from AND %s/time/value < $to
            """.formatted(PULSE_EVENT, PULSE_EVENT);

    // EHRbase requires every ORDER BY path to appear in the SELECT, hence the second column.
    private static final String COMPOSITIONS_AQL = """
            SELECT c AS composition, %s/time/value AS measured_at
            FROM EHR e[ehr_id/value=$ehrId]
              CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.encounter.v1]
                CONTAINS OBSERVATION o[openEHR-EHR-OBSERVATION.pulse.v2]
            WHERE %s/time/value >= $from
            ORDER BY %s/time/value ASC
            """.formatted(PULSE_EVENT, PULSE_EVENT, PULSE_EVENT);

    private final EhrbaseClient ehrbase;
    private final OpenFhirClient openFhir;
    private final PulseObservations pulseObservations;
    private final HeartRateExtractor extractor;
    private final HeartrateProperties properties;
    private final EhrResolver ehrResolver;
    private final PatientDirectory directory;
    private final ObjectMapper objectMapper;

    public HeartRateService(
            EhrbaseClient ehrbase,
            OpenFhirClient openFhir,
            PulseObservations pulseObservations,
            HeartRateExtractor extractor,
            HeartrateProperties properties,
            EhrResolver ehrResolver,
            PatientDirectory directory,
            ObjectMapper objectMapper) {
        this.ehrbase = ehrbase;
        this.openFhir = openFhir;
        this.pulseObservations = pulseObservations;
        this.extractor = extractor;
        this.properties = properties;
        this.ehrResolver = ehrResolver;
        this.directory = directory;
        this.objectMapper = objectMapper;
    }

    /** Stores an Observation, which is how everything gets into the CDR. */
    public Observation record(Observation observation, String patientId) {
        var ehrId = ehrResolver.ehrIdFor(patientId);
        // A client is entitled to post an Observation without an id and let the server assign one.
        // Without this the Bundle's fullUrl and the 201's Location header both read "urn:uuid:null".
        if (!observation.hasIdElement() || observation.getIdElement().getIdPart() == null) {
            observation.setId(java.util.UUID.randomUUID().toString());
        }
        var bundle = pulseObservations.bundle(List.of(observation));
        var composition = openFhir.toOpenEhr(pulseObservations.encode(bundle), properties.templateId());
        if (composition == null || !composition.has("content") || composition.get("content").isEmpty()) {
            throw new IllegalStateException(
                    "openFHIR produced no composition content; the Observation did not match the FHIR Connect mapping");
        }
        // A second reading for a day that already has one is a correction, not a second fact. openEHR
        // expresses that by versioning the composition rather than adding another one — and the old
        // version stays in the record, which is the whole point of a clinical data repository.
        var existing = measuredAt(observation).flatMap(at -> latestVersionOn(at, ehrId));
        String uid;
        if (existing.isPresent()) {
            var precedingVersion = existing.get();
            uid = ehrbase.updateComposition(
                    ehrId, versionedObjectUid(precedingVersion), precedingVersion, composition);
            log.debug("Corrected resting heart rate to {}: {} is now {}",
                    observation.getValueQuantity().getValue(), precedingVersion, uid);
        } else {
            uid = ehrbase.createComposition(ehrId, composition);
            log.debug("Stored resting heart rate {} as composition {}",
                    observation.getValueQuantity().getValue(), uid);
        }
        // The server assigns the id, as a FHIR server may, and the id it assigns is the openEHR
        // composition's own. Whatever id the client proposed is dropped here — keeping it would mean
        // holding a table that translates between the two, and there is nothing that table would
        // know which the uid does not already say.
        if (uid != null) {
            observation.setId(versionedObjectUid(uid));
            observation.getMeta().setVersionId(versionNumber(uid));
        }
        return observation;
    }

    /**
     * One stored reading, addressed the way FHIR addresses a resource.
     *
     * <p>No index stands behind this. The id <em>is</em> the openEHR versioned object uid, so the
     * lookup is a composition read rather than a translation — which is only possible because this
     * service generates the outgoing FHIR and can therefore choose the id.
     *
     * <p>openEHR addresses a composition as (EHR, uid) while FHIR addresses a resource by id alone,
     * so the record has to be named from outside or found. Naming the patient is one read; leaving
     * it out costs a scan of the roster, which is honest at two patients and would not be at two
     * million.
     */
    public Optional<Observation> read(String observationId, String patientId) {
        var found = readFrom(observationId, patientId);
        if (found.isPresent()) {
            return found;
        }
        for (var other : directory.roster()) {
            if (other.id().equals(patientId)) {
                continue;
            }
            var elsewhere = readFrom(observationId, other.id());
            if (elsewhere.isPresent()) {
                return elsewhere;
            }
        }
        return Optional.empty();
    }

    private Optional<Observation> readFrom(String observationId, String patientId) {
        JsonNode composition;
        try {
            composition = ehrbase.getComposition(ehrResolver.ehrIdFor(patientId), observationId);
        } catch (Exception e) {
            log.debug("No composition {} in {}'s record: {}", observationId, patientId, e.getMessage());
            return Optional.empty();
        }
        if (composition == null || composition.isNull()) {
            return Optional.empty();
        }
        return mapToObservations(composition, patientId).stream().findFirst();
    }

    private Optional<OffsetDateTime> measuredAt(Observation observation) {
        var effective = observation.getEffectiveDateTimeType();
        if (effective == null || effective.getValue() == null) {
            return Optional.empty();
        }
        return Optional.of(effective.getValue().toInstant().atOffset(ZoneOffset.UTC));
    }

    /** The most recently committed composition covering the day of {@code measuredAt}, if any. */
    private Optional<String> latestVersionOn(OffsetDateTime measuredAt, String ehrId) {
        var day = measuredAt.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
        var parameters = Map.of(
                "ehrId", (Object) ehrId,
                "from", startOfDay(day),
                "to", startOfDay(day.plusDays(1)));

        List<List<JsonNode>> rows;
        try {
            rows = ehrbase.query(SAME_DAY_AQL, parameters);
        } catch (Exception e) {
            // Not being able to look for a previous version is no reason to lose the reading; the
            // worst case is the day carrying two compositions, and series() still picks the newest.
            log.warn("Could not check for an existing composition on {}: {}", day, e.getMessage());
            return Optional.empty();
        }

        return rows.stream()
                .filter(row -> row.size() >= 2 && !row.get(0).isNull() && !row.get(1).isNull())
                .max(Comparator.comparing(row -> committedAt(row.get(1))))
                .map(row -> row.get(0).asText());
    }

    /** {@code <uuid>::<node>::<version>} addresses a version; the uuid alone addresses the object. */
    private static String versionedObjectUid(String versionUid) {
        int marker = versionUid.indexOf("::");
        return marker < 0 ? versionUid : versionUid.substring(0, marker);
    }

    /** The trailing {@code ::n} of a version uid, which is what FHIR calls meta.versionId. */
    private static String versionNumber(String versionUid) {
        int marker = versionUid.lastIndexOf("::");
        return marker < 0 ? null : versionUid.substring(marker + 2);
    }

    /** Commit times are ISO-8601 but not uniformly precise, so they are parsed rather than compared as text. */
    private static OffsetDateTime committedAt(JsonNode value) {
        try {
            return OffsetDateTime.parse(value.asText());
        } catch (Exception e) {
            return OffsetDateTime.MIN;
        }
    }

    /** The outcome of one import, in the shape the FHIR layer turns into an OperationOutcome. */
    public record ImportResult(int imported, List<HeartRateExtractor.Rejection> rejections) {}

    /** Imports a FHIR Bundle, keeping the entries that are usable resting heart rates. */
    public ImportResult importJson(String json, String patientId) {
        Bundle bundle;
        try {
            bundle = pulseObservations.parse(json, Bundle.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("That is not a FHIR Bundle.", e);
        }
        var extraction = extractor.fromBundle(bundle);
        return store(extraction.readings(), new ArrayList<>(extraction.rejections()), patientId);
    }

    private ImportResult store(
            List<Reading> readings, List<HeartRateExtractor.Rejection> failures, String patientId) {
        int imported = 0;
        for (var reading : readings) {
            try {
                record(pulseObservations.observation(reading, patientId), patientId);
                imported++;
            } catch (Exception e) {
                log.warn("Could not import a reading: {}", e.getMessage());
                failures.add(new HeartRateExtractor.Rejection("Observation", "could not be stored"));
            }
        }
        log.info("Imported {} resting heart rates", imported);
        return new ImportResult(imported, List.copyOf(failures));
    }

    /** The daily resting heart rates of the last {@code days} days. */
    public HeartRateSeries series(int days, String patientId) {
        var to = LocalDate.now(ZoneOffset.UTC);
        var from = to.minusDays(days - 1L);

        var rows = ehrbase.query(READINGS_AQL, queryParameters(from, ehrResolver.ehrIdFor(patientId)));
        return new HeartRateSeries(from, to, newestPerDay(rows));
    }

    /**
     * Folds the query result to one value per day, keeping the one committed last.
     *
     * <p>A day can carry several compositions — a correction made before this service versioned them,
     * or a file imported twice — and the store is under no obligation to return them newest last.
     * Taking whichever arrived last in the result means the first value ever written wins and every
     * correction after it silently disappears.
     *
     * @param rows {@code measured_at, bpm, committed}, as {@link #READINGS_AQL} selects them
     */
    static List<DailyRestingHeartRate> newestPerDay(List<List<JsonNode>> rows) {
        record Recorded(OffsetDateTime committed, double bpm) {}

        var byDay = new java.util.HashMap<LocalDate, Recorded>();
        for (var row : rows) {
            if (row.size() < 3 || row.get(0).isNull() || row.get(1).isNull()) {
                continue;
            }
            var date = OffsetDateTime.parse(row.get(0).asText())
                    .atZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDate();
            var candidate = new Recorded(committedAt(row.get(2)), row.get(1).asDouble());
            byDay.merge(date, candidate,
                    (kept, fresh) -> fresh.committed().isAfter(kept.committed()) ? fresh : kept);
        }

        return byDay.entrySet().stream()
                .map(entry -> new DailyRestingHeartRate(entry.getKey(), entry.getValue().bpm()))
                .sorted(Comparator.comparing(DailyRestingHeartRate::date))
                .toList();
    }

    /**
     * Exports the stored readings as a FHIR searchset Bundle. Each composition goes back through
     * openFHIR, so the outgoing FHIR is produced by the same mapping definition as the incoming FHIR.
     */
    public Bundle export(int days, String patientId) {
        var from = LocalDate.now(ZoneOffset.UTC).minusDays(days - 1L);
        var rows = ehrbase.query(
                COMPOSITIONS_AQL, queryParameters(from, ehrResolver.ehrIdFor(patientId)));

        var observations = new ArrayList<Observation>();
        for (var row : rows) {
            if (row.isEmpty() || row.get(0).isNull()) {
                continue;
            }
            observations.addAll(mapToObservations(row.get(0), patientId));
        }

        var bundle = pulseObservations.bundle(observations);
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal(observations.size());
        return bundle;
    }

    private List<Observation> mapToObservations(JsonNode composition, String patientId) {
        JsonNode mapped;
        try {
            mapped = openFhir.toFhir(objectMapper.writeValueAsString(composition), properties.templateId());
        } catch (Exception e) {
            log.warn("openFHIR could not map a stored composition back to FHIR: {}", e.getMessage());
            return List.of();
        }
        if (mapped == null) {
            return List.of();
        }

        // openFHIR answers with a Bundle whose entries are the mapped Observations.
        var observations = new ArrayList<Observation>();
        for (var entry : mapped.path("entry")) {
            var resource = entry.path("resource");
            if ("Observation".equals(resource.path("resourceType").asText())) {
                var observation = pulseObservations.parseObservation(resource.toString());
                // openFHIR hands back an id of its own making; the composition's uid is the better
                // one, because it is what the record is actually addressed by. meta.versionId comes
                // from the same uid, which is how openEHR's versioning reaches FHIR at all: correct
                // a reading and the id stays while the version moves.
                var uid = composition.path("uid").path("value").asText(null);
                if (uid != null) {
                    observation.setId(versionedObjectUid(uid));
                    observation.getMeta().setVersionId(versionNumber(uid));
                } else {
                    var id = observation.getIdElement().getIdPart();
                    if (id != null && id.startsWith("#")) {
                        observation.setId(id.substring(1));
                    }
                }
                observation.setSubject(
                        new org.hl7.fhir.r4.model.Reference("Patient/" + patientId));
                observations.add(observation);
            }
        }
        return observations;
    }

    private Map<String, Object> queryParameters(LocalDate from, String ehrId) {
        return Map.of(
                "ehrId", ehrId,
                "from", startOfDay(from));
    }

    /**
     * Midnight UTC, written out in full.
     *
     * <p>EHRbase compares DV_DATE_TIME bounds as text, and {@code OffsetDateTime.toString()} leaves
     * the seconds out when they are zero — "2026-09-08T00:00Z" rather than "2026-09-08T00:00:00Z".
     * That is valid ISO-8601 and still silently matches nothing. {@code Instant.toString()} always
     * writes the seconds.
     */
    static String startOfDay(LocalDate day) {
        return day.atStartOfDay().toInstant(ZoneOffset.UTC).toString();
    }
}
