package com.example.heartrate.web;

import com.example.heartrate.fhir.PulseObservations;
import com.example.heartrate.patient.EhrResolver;
import com.example.heartrate.patient.PatientDirectory;
import com.example.heartrate.patient.PatientResources;
import java.util.List;
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
 * <p>Nothing is stored: each resource is projected from the configured directory entry and the EHR
 * id openEHR answers with. That is enough to make {@code Observation.subject} resolve — it used to
 * point at a Patient that existed nowhere — and it is deliberately the smallest thing that does so.
 */
@RestController
@RequestMapping("/fhir/Patient")
public class PatientController {

    private static final String FHIR_JSON = "application/fhir+json";

    private final PatientDirectory directory;
    private final EhrResolver ehrResolver;
    private final PatientResources resources;
    private final PulseObservations fhir;

    public PatientController(
            PatientDirectory directory,
            EhrResolver ehrResolver,
            PatientResources resources,
            PulseObservations fhir) {
        this.directory = directory;
        this.ehrResolver = ehrResolver;
        this.resources = resources;
        this.fhir = fhir;
    }

    @GetMapping(produces = FHIR_JSON)
    public ResponseEntity<String> search() {
        var bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        List<org.hl7.fhir.r4.model.Patient> all = directory.all().stream()
                .map(patient -> resources.toFhir(patient, ehrResolver.ehrIdFor(patient.id())))
                .toList();
        all.forEach(patient -> bundle.addEntry().setResource(patient));
        bundle.setTotal(all.size());
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(fhir.encode(bundle));
    }

    @GetMapping(value = "/{id}", produces = FHIR_JSON)
    public ResponseEntity<String> read(@PathVariable String id) {
        var patient = directory.byId(id);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf(FHIR_JSON))
                .body(fhir.encode(resources.toFhir(patient, ehrResolver.ehrIdFor(patient.id()))));
    }
}
