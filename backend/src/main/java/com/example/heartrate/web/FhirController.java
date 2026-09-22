package com.example.heartrate.web;

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
    private final SampleReadings sampleReadings;
    private final PatientDirectory patients;

    public FhirController(
            HeartRateService service,
            PulseObservations pulseObservations,
            SampleReadings sampleReadings,
            PatientDirectory patients) {
        this.service = service;
        this.pulseObservations = pulseObservations;
        this.sampleReadings = sampleReadings;
        this.patients = patients;
    }

    /** Ingests a vital-signs heart rate Observation from an external system. */
    @PostMapping(value = "/Observation", consumes = {FHIR_JSON, MediaType.APPLICATION_JSON_VALUE},
            produces = FHIR_JSON)
    public ResponseEntity<String> create(
            @RequestBody String observationJson,
            @RequestParam(required = false) String patient) {
        var stored = service.record(
                pulseObservations.parseObservation(observationJson), patients.resolve(patient));
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
