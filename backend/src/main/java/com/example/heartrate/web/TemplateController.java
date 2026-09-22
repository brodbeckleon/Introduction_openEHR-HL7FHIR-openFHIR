package com.example.heartrate.web;

import com.example.heartrate.patient.PatientDirectory;
import com.example.heartrate.template.TemplateService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The operational template as a tree, marked with what the stored data actually uses.
 *
 * <p>Not a FHIR endpoint: a template is an openEHR artefact with no FHIR counterpart.
 */
@RestController
@RequestMapping("/api/template")
public class TemplateController {

    private final TemplateService service;
    private final PatientDirectory patients;

    public TemplateController(TemplateService service, PatientDirectory patients) {
        this.service = service;
        this.patients = patients;
    }

    @GetMapping
    public TemplateService.TemplateView describe(@RequestParam(required = false) String patient) {
        return service.describe(patients.resolve(patient));
    }
}
