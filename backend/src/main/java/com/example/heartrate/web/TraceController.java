package com.example.heartrate.web;

import com.example.heartrate.config.Messages;
import com.example.heartrate.patient.PatientDirectory;
import com.example.heartrate.trace.Trace;
import com.example.heartrate.trace.TraceService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The pipeline inspector: the same work {@code /fhir/Bundle} does, with nothing thrown away.
 *
 * <p>Not part of the FHIR API — this exists so the UI can show what the standards do to a reading,
 * which is the point of the whole demo.
 */
@RestController
@RequestMapping("/api/trace")
public class TraceController {

    private final TraceService service;
    private final ObjectMapper objectMapper;
    private final Messages messages;
    private final PatientDirectory patients;

    public TraceController(
            TraceService service,
            ObjectMapper objectMapper,
            Messages messages,
            PatientDirectory patients) {
        this.service = service;
        this.objectMapper = objectMapper;
        this.messages = messages;
        this.patients = patients;
    }

    /**
     * Runs one reading through the pipeline.
     *
     * @param store when true the composition is really written to EHRbase and read back with AQL;
     *     by default the trace is a dry run and the record is left alone
     */
    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE, "application/fhir+json"})
    public Trace trace(
            @RequestBody String json,
            @RequestParam(defaultValue = "false") boolean store,
            @RequestParam(required = false) String patient) {
        return service.trace(json, store, patients.resolve(patient));
    }

    /**
     * Ready-made inputs, each chosen to show one thing the standards do.
     *
     * <p>The file holds message keys rather than text, and {@code ${today}} rather than a date. Both
     * are resolved here: the text so the samples arrive in the same language as everything else, the
     * dates so a sample is never about a day three months ago. A reading dated today also lands on
     * the same day as one typed into the entry form, which is what lets the tour follow one number
     * from the form into the pipeline.
     */
    @GetMapping("/samples")
    public JsonNode samples() throws Exception {
        var today = LocalDate.now(ZoneOffset.UTC);
        var text = new ClassPathResource("trace-samples.json")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("${today}", midnightUtc(today))
                .replace("${yesterday}", midnightUtc(today.minusDays(1)));

        var samples = objectMapper.readTree(text);
        for (JsonNode sample : samples) {
            var object = (com.fasterxml.jackson.databind.node.ObjectNode) sample;
            object.put("label", messages.get(object.path("labelKey").asText()));
            object.put("summary", messages.get(object.path("summaryKey").asText()));
            object.remove("labelKey");
            object.remove("summaryKey");
        }
        return samples;
    }

    /** Midnight UTC: a resting heart rate is a whole day's value, not a moment in one. */
    private static String midnightUtc(LocalDate day) {
        return day.atStartOfDay().toInstant(ZoneOffset.UTC).toString();
    }
}
