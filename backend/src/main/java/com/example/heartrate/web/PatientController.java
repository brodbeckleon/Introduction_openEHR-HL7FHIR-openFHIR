package com.example.heartrate.web;

import ca.uhn.fhir.parser.DataFormatException;
import com.example.heartrate.fhir.PulseObservations;
import com.example.heartrate.patient.PatientDirectory;
import com.example.heartrate.service.HeartRateService;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The patients this instance knows, as FHIR — read, found by identifier, and updated.
 *
 * <p>They live in the FHIR store, this service's own database, which is where a patient's details
 * belong once the patient exists. Every request goes through the roster first: the store would
 * happily hold patients this demo knows nothing about, and an id outside the roster is a 404 here
 * rather than a new record.
 */
@RestController
@RequestMapping("/fhir/Patient")
public class PatientController {

    private static final String FHIR_JSON = "application/fhir+json";

    private final PatientDirectory directory;
    private final HeartRateService service;
    private final PulseObservations fhir;

    public PatientController(
            PatientDirectory directory, HeartRateService service, PulseObservations fhir) {
        this.directory = directory;
        this.service = service;
        this.fhir = fhir;
    }

    /**
     * Every patient; with {@code identifier}, only those carrying it.
     *
     * <p>The token is {@code system|value}, or a bare value matching any system, as FHIR search
     * writes it. The EHR id travels as one of these identifiers, so this is how a caller holding an
     * openEHR record finds the patient it belongs to.
     */
    @GetMapping(produces = FHIR_JSON)
    public ResponseEntity<String> search(@RequestParam(required = false) String identifier) {
        var bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        var found = directory.all().stream()
                .filter(patient -> identifier == null || carries(patient, identifier))
                .toList();
        found.forEach(patient -> bundle.addEntry().setResource(patient));
        bundle.setTotal(found.size());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(fhir.encode(bundle));
    }

    /**
     * An ordinary FHIR update: replaces what the FHIR store holds about one patient.
     *
     * <p>Only for patients of the roster, like every other request. An id in the body has to be the
     * one in the URL. The link to the openEHR record is re-attached whatever the body says — see
     * {@link com.example.heartrate.patient.PatientResources#withEhrIdentifier}. 201 when the store
     * did not hold the patient yet, 200 when it replaced them, 503 when the store cannot be reached;
     * the readings are untouched either way.
     */
    @PutMapping(value = "/{id}", consumes = {FHIR_JSON, MediaType.APPLICATION_JSON_VALUE},
            produces = FHIR_JSON)
    public ResponseEntity<String> update(@PathVariable String id, @RequestBody String body) {
        var patientId = directory.resolve(id);
        Patient patient;
        try {
            patient = fhir.parse(body, Patient.class);
        } catch (DataFormatException e) {
            return problem(HttpStatus.BAD_REQUEST, OperationOutcome.IssueType.STRUCTURE,
                    "Not a FHIR Patient: " + e.getMessage());
        }
        var sentId = patient.getIdElement().getIdPart();
        if (sentId != null && !sentId.equals(patientId)) {
            return problem(HttpStatus.BAD_REQUEST, OperationOutcome.IssueType.INVALID,
                    "The id in the body (%s) is not the id in the URL (%s).".formatted(sentId, patientId));
        }
        patient.setId(patientId);

        boolean created;
        try {
            created = directory.save(patient);
        } catch (DataAccessException e) {
            return problem(HttpStatus.SERVICE_UNAVAILABLE, OperationOutcome.IssueType.TRANSIENT,
                    "The FHIR store cannot be reached, so nothing was changed. The readings in openEHR "
                            + "are not affected.");
        }
        return ResponseEntity.status(created ? HttpStatus.CREATED : HttpStatus.OK)
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(fhir.encode(directory.byId(patientId)));
    }

    /**
     * Everything known about a patient, in one Bundle — and assembled from two stores.
     *
     * <p>This is the whole arrangement in a single response. The Patient comes from the FHIR store,
     * which is the only place a name or an address exists. The Observations come from EHRbase as
     * openEHR compositions and are mapped back to FHIR by openFHIR on the way out. Neither store
     * holds the other's half, and nothing but this method ever sees both.
     *
     * <p>The reply is ordinary FHIR with no marker saying which entry came from where. That is
     * deliberate: a client should not have to care, and the moment the Bundle admitted it, the
     * arrangement would have leaked into the interface it exists to hide.
     */
    @GetMapping(value = "/{id}/$everything", produces = FHIR_JSON)
    public ResponseEntity<String> everything(
            @PathVariable String id, @RequestParam(defaultValue = "30") int days) {
        var patientId = directory.resolve(id);

        var bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.addEntry().setResource(directory.byId(patientId));
        service.export(Math.clamp(days, 1, 365), patientId).getEntry()
                .forEach(entry -> bundle.addEntry().setResource(entry.getResource()));
        bundle.setTotal(bundle.getEntry().size());

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(fhir.encode(bundle));
    }

    @GetMapping(value = "/{id}", produces = FHIR_JSON)
    public ResponseEntity<String> read(@PathVariable String id) {
        // Through the directory, so an id outside the roster is a 404 here even when the store has it.
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(fhir.encode(directory.byId(directory.resolve(id))));
    }

    /** Whether the patient carries the identifier a FHIR token search names. */
    static boolean carries(Patient patient, String token) {
        var bar = token.indexOf('|');
        var system = bar < 0 ? null : token.substring(0, bar);
        var value = bar < 0 ? token : token.substring(bar + 1);
        return patient.getIdentifier().stream().anyMatch(identifier -> value.equals(identifier.getValue())
                && (system == null || system.equals(identifier.getSystem())));
    }

    private ResponseEntity<String> problem(
            HttpStatus status, OperationOutcome.IssueType type, String diagnostics) {
        var outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(type)
                .setDiagnostics(diagnostics);
        return ResponseEntity.status(status)
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(fhir.encode(outcome));
    }
}
