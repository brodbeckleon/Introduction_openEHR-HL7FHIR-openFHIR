package com.example.heartrate.web;

import com.example.heartrate.fhir.PulseObservations;
import com.example.heartrate.patient.PatientDirectory;
import com.example.heartrate.service.HeartRateService;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The patients this instance knows, as FHIR.
 *
 * <p>These come from the FHIR store, which is where a patient's details live. This endpoint stays
 * here rather than sending clients straight to that server because the roster is this app's — the
 * store would happily answer with patients this demo knows nothing about.
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

    @GetMapping(produces = FHIR_JSON)
    public ResponseEntity<String> search() {
        var bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        var all = directory.all();
        all.forEach(patient -> bundle.addEntry().setResource(patient));
        bundle.setTotal(all.size());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(fhir.encode(bundle));
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
}
