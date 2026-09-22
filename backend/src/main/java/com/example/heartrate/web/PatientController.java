package com.example.heartrate.web;

import com.example.heartrate.fhir.PulseObservations;
import com.example.heartrate.patient.PatientDirectory;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
    private final PulseObservations fhir;

    public PatientController(PatientDirectory directory, PulseObservations fhir) {
        this.directory = directory;
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

    @GetMapping(value = "/{id}", produces = FHIR_JSON)
    public ResponseEntity<String> read(@PathVariable String id) {
        // Through the directory, so an id outside the roster is a 404 here even when the store has it.
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(fhir.encode(directory.byId(directory.resolve(id))));
    }
}
