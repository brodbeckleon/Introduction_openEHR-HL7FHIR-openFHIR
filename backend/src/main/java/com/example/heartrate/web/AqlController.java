package com.example.heartrate.web;

import com.example.heartrate.aql.AqlResult;
import com.example.heartrate.aql.AqlService;
import com.example.heartrate.config.Messages;
import com.example.heartrate.patient.PatientDirectory;
import com.example.heartrate.service.HeartRateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The AQL playground: run a query against the record and see what openEHR answers.
 *
 * <p>Read-only, and not part of the FHIR API — AQL is openEHR's own query language and has no
 * counterpart on the FHIR side.
 */
@RestController
@RequestMapping("/api/aql")
public class AqlController {

    private final AqlService service;
    private final ObjectMapper objectMapper;
    private final Messages messages;
    private final PatientDirectory patients;

    public AqlController(
            AqlService service,
            ObjectMapper objectMapper,
            Messages messages,
            PatientDirectory patients) {
        this.service = service;
        this.objectMapper = objectMapper;
        this.messages = messages;
        this.patients = patients;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public AqlResult run(@RequestBody String query, @RequestParam(required = false) String patient) {
        return service.run(query, patients.resolve(patient));
    }

    /** Ready-made queries, each chosen to show one thing about AQL. */
    @GetMapping("/examples")
    public JsonNode examples() throws Exception {
        var examples = objectMapper.readTree(
                new ClassPathResource("aql-examples.json").getContentAsString(StandardCharsets.UTF_8));
        for (JsonNode example : examples) {
            var object = (ObjectNode) example;
            var id = object.path("id").asText();
            object.put("label", messages.get("aql." + id + ".label"));
            object.put("teaches", messages.get("aql." + id + ".teaches"));
            // The chart's query is the real one, taken from where the chart takes it. A copy in
            // the file would be the query the chart used to run.
            if ("chart".equals(id)) {
                object.put("query", HeartRateService.READINGS_AQL.strip());
            }
        }
        return examples;
    }
}
