package com.example.heartrate.web;

import com.example.heartrate.fhir.HeartRateExtractor;
import com.example.heartrate.fhir.PulseObservations;
import com.example.heartrate.fhir.SampleReadings;
import com.example.heartrate.patient.PatientDirectory;
import com.example.heartrate.service.HeartRateService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The HL7 FHIR face of the service: how other systems exchange heart rate data with it.
 *
 * <p>Both directions go through the same openFHIR mappings as the UI does.
 */
@RestController
@RequestMapping("/fhir")
public class FhirController {

    private static final String FHIR_JSON = "application/fhir+json";

    private final HeartRateService service;
    private final PulseObservations pulseObservations;
    private final HeartRateExtractor extractor;
    private final SampleReadings sampleReadings;
    private final PatientDirectory patients;

    public FhirController(
            HeartRateService service,
            PulseObservations pulseObservations,
            HeartRateExtractor extractor,
            SampleReadings sampleReadings,
            PatientDirectory patients) {
        this.service = service;
        this.pulseObservations = pulseObservations;
        this.extractor = extractor;
        this.sampleReadings = sampleReadings;
        this.patients = patients;
    }

    /**
     * Ingests a vital-signs heart rate Observation from an external system.
     *
     * <p>The same road a Bundle entry takes: the Observation is recognised and rebuilt in canonical
     * form before it reaches the mappings, so {@code bpm} and SNOMED are as welcome here as they are
     * in an import, and one that is not a resting heart rate is refused with a reason rather than
     * handed to openFHIR to fail on.
     */
    @PostMapping(value = "/Observation", consumes = {FHIR_JSON, MediaType.APPLICATION_JSON_VALUE},
            produces = FHIR_JSON)
    public ResponseEntity<String> create(
            @RequestBody String observationJson,
            @RequestParam(required = false) String patient) {
        var patientId = patients.resolve(patient);
        var arrived = pulseObservations.parseObservation(observationJson);
        var reading = extractor.reading(arrived);
        if (reading.isEmpty()) {
            var outcome = new OperationOutcome();
            outcome.addIssue()
                    .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                    .setCode(OperationOutcome.IssueType.NOTSUPPORTED)
                    .setDiagnostics("Not a usable resting heart rate: " + extractor.rejectionReason(arrived));
            return ResponseEntity.unprocessableEntity()
                    .contentType(MediaType.valueOf(FHIR_JSON))
                    .body(pulseObservations.encode(outcome));
        }
        var stored = service.record(pulseObservations.observation(reading.get(), patientId), patientId);
        return ResponseEntity.created(java.net.URI.create("Observation/" + stored.getIdElement().getIdPart()))
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(pulseObservations.encode(stored));
    }

    /**
     * Imports a FHIR Bundle — this service's own export, or any other Bundle carrying resting heart
     * rates. The answer is an OperationOutcome saying what went in and what did not.
     */
    @PostMapping(value = "/Bundle", consumes = {FHIR_JSON, MediaType.APPLICATION_JSON_VALUE},
            produces = FHIR_JSON)
    public ResponseEntity<String> importBundle(
            @RequestBody String bundleJson,
            @RequestParam(required = false) String patient) {
        var result = service.importJson(bundleJson, patients.resolve(patient));
        return ResponseEntity.status(result.imported() > 0 ? 201 : 200)
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(pulseObservations.encode(outcomeOf(result)));
    }

    /** Turns the import result into an OperationOutcome, one issue per distinct reason. */
    private OperationOutcome outcomeOf(HeartRateService.ImportResult result) {
        var outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(result.imported() > 0
                        ? OperationOutcome.IssueSeverity.INFORMATION
                        : OperationOutcome.IssueSeverity.WARNING)
                .setCode(OperationOutcome.IssueType.INFORMATIONAL)
                .setDiagnostics("Imported %d heart rate observation%s."
                        .formatted(result.imported(), result.imported() == 1 ? "" : "s"));

        // Group the rejections so a 500-entry bundle does not produce 500 issues.
        Map<String, Integer> grouped = new LinkedHashMap<>();
        for (var rejection : result.rejections()) {
            grouped.merge("%s (%s)".formatted(rejection.resourceType(), rejection.reason()), 1, Integer::sum);
        }
        grouped.forEach((reason, count) -> outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.WARNING)
                .setCode(OperationOutcome.IssueType.NOTSUPPORTED)
                .setDiagnostics("Skipped %d %s".formatted(count, reason)));

        return outcome;
    }

    /**
     * A month of made-up resting heart rates as a FHIR Bundle, ending today.
     *
     * <p>Nothing is stored: this hands back the same shape an external system would send, and it is
     * imported through {@code POST /fhir/Bundle} like anything else. Generated on request rather
     * than checked in, because a file with fixed dates falls out of the chart's 30-day window and
     * would have to be regenerated to stay useful.
     */
    @GetMapping(value = "/Bundle/$sample", produces = FHIR_JSON)
    public ResponseEntity<String> sample(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String patient) {
        var patientId = patients.resolve(patient);
        var readings = sampleReadings.lastDays(Math.clamp(days, 1, 365));
        var bundle = pulseObservations.bundle(
                readings.stream().map(reading -> pulseObservations.observation(reading, patientId)).toList());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(pulseObservations.encode(bundle));
    }

    /**
     * One stored reading by its id.
     *
     * <p>The id is the openEHR composition's versioned object uid — this service assigns it on
     * create rather than keeping the one a client proposed, which is what makes the read possible
     * without an index translating between the two addressing schemes.
     */
    @GetMapping(value = "/Observation/{id}", produces = FHIR_JSON)
    public ResponseEntity<String> read(
            @PathVariable String id, @RequestParam(required = false) String patient) {
        return service.read(id, patients.resolve(patient))
                .map(observation -> ResponseEntity.ok()
                        .contentType(MediaType.valueOf(FHIR_JSON))
                        .body(pulseObservations.encode(observation)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Everything stored in the last {@code days} days, as a FHIR searchset Bundle. */
    @GetMapping(value = "/Observation", produces = FHIR_JSON)
    public ResponseEntity<String> search(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) String patient) {
        var bundle = service.export(Math.clamp(days, 1, 365), patients.resolve(patient));
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(pulseObservations.encode(bundle));
    }
}
