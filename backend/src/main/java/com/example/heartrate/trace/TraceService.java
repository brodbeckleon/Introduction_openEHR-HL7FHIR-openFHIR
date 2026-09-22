package com.example.heartrate.trace;

import com.example.heartrate.config.HeartrateProperties;
import com.example.heartrate.config.Messages;
import com.example.heartrate.fhir.HeartRateExtractor;
import com.example.heartrate.fhir.PulseObservations;
import com.example.heartrate.model.Reading;
import com.example.heartrate.openehr.EhrbaseClient;
import com.example.heartrate.patient.EhrResolver;
import com.example.heartrate.patient.PatientDirectory;
import com.example.heartrate.openfhir.OpenFhirClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

/**
 * Runs one reading through the whole pipeline and keeps every intermediate representation.
 *
 * <p>{@link com.example.heartrate.service.HeartRateService} does the same work but throws the
 * intermediate forms away, which is right for an import and wrong for learning: the interesting part
 * of this project is what a resting heart rate looks like as FHIR, what openFHIR turns it into, and
 * what openEHR insists on that FHIR never carried. This service exists to make that visible.
 *
 * <p>A trace is a dry run by default — nothing is written to EHRbase unless the caller asks for it.
 */
@Service
public class TraceService {

    private static final String AQL = """
            SELECT o/data[at0002]/events[at0003]/time/value AS measured_at,
                   o/data[at0002]/events[at0003]/data[at0001]/items[at0004]/value/magnitude AS bpm
            FROM EHR e[ehr_id/value=$ehrId]
              CONTAINS COMPOSITION c[openEHR-EHR-COMPOSITION.encounter.v1]
                CONTAINS OBSERVATION o[openEHR-EHR-OBSERVATION.pulse.v2]
            ORDER BY o/data[at0002]/events[at0003]/time/value DESC
            """;

    private final OpenFhirClient openFhir;
    private final EhrbaseClient ehrbase;
    private final PulseObservations pulseObservations;
    private final HeartRateExtractor extractor;
    private final MappingLibrary mappings;
    private final Messages messages;
    private final HeartrateProperties properties;
    private final EhrResolver ehrResolver;
    private final PatientDirectory patients;
    private final ObjectMapper objectMapper;

    public TraceService(
            OpenFhirClient openFhir,
            EhrbaseClient ehrbase,
            PulseObservations pulseObservations,
            HeartRateExtractor extractor,
            MappingLibrary mappings,
            Messages messages,
            HeartrateProperties properties,
            EhrResolver ehrResolver,
            PatientDirectory patients,
            ObjectMapper objectMapper) {
        this.openFhir = openFhir;
        this.ehrbase = ehrbase;
        this.pulseObservations = pulseObservations;
        this.extractor = extractor;
        this.mappings = mappings;
        this.messages = messages;
        this.properties = properties;
        this.ehrResolver = ehrResolver;
        this.patients = patients;
        this.objectMapper = objectMapper;
    }

    public Trace trace(String json, boolean store, String patientId) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("That is not JSON.", e);
        }

        var steps = new ArrayList<TraceStep>();
        var recognised = recognise(root, steps);
        if (recognised.isEmpty()) {
            return new Trace(kindOf(root), labelOf(root), false, List.copyOf(steps), List.of(), mappings.sources());
        }
        var reading = recognised.get();

        // 2. The canonical FHIR Observation. Whatever shape the reading arrived in, everything
        //    downstream sees exactly this.
        var observation = pulseObservations.observation(reading, patientId);
        steps.add(TraceStep.of("observation", messages.get("step.observation"), messages.get("actor.backend"),
                "fhir", messages.get("explain.observation"), tree(pulseObservations.encode(observation))));


        // 3. The Bundle. FHIR Connect anchors the context mapping on a Bundle, so even a single
        //    reading travels as a one-entry collection.
        var bundle = pulseObservations.bundle(List.of(observation));
        var bundleJson = pulseObservations.encode(bundle);
        steps.add(TraceStep.of("bundle", messages.get("step.bundle"), messages.get("actor.backend"), "fhir",
                messages.get("explain.bundle"), tree(bundleJson)));

        // 4. openFHIR executes the mappings.
        JsonNode composition;
        long started = System.nanoTime();
        try {
            composition = openFhir.toOpenEhr(bundleJson, properties.templateId());
        } catch (Exception e) {
            steps.add(TraceStep.failed("composition", messages.get("step.composition"), "openFHIR",
                    messages.get("explain.composition.short"), messages.get("fail.openfhir", e.getMessage())));
            return assemble(root, false, steps, null);
        }
        long tookMs = (System.nanoTime() - started) / 1_000_000;

        if (composition == null || !composition.has("content") || composition.get("content").isEmpty()) {
            steps.add(TraceStep.failed("composition", messages.get("step.composition"), "openFHIR",
                    messages.get("explain.composition.short"), messages.get("fail.openfhir.empty")));
            return assemble(root, false, steps, null);
        }

        steps.add(TraceStep.of("composition", messages.get("step.composition"), "openFHIR", "openehr",
                messages.get("explain.composition"), composition)
                .withCall("POST /openfhir/toopenehr?templateId=%s&format=canonical"
                        .formatted(properties.templateId()), tookMs));

        // 5. Where it goes. Not a FHIR question at all: openEHR anchors an EHR on
        //    EHR_STATUS.subject, so the answer comes out of openEHR itself. It sits here rather
        //    than earlier because it is the address for the write, not part of building the
        //    Bundle — and on a dry run it is the last thing the way in has to say.
        addEhrStep(steps, patientId);

        // 6. and 7. Persistence, and reading back with AQL — only when explicitly asked for.
        if (store) {
            storeAndQuery(composition, steps, ehrResolver.ehrIdFor(patientId));
        }

        // 7. The way back — a different operation from everything above it, which is why the
        //    inspector shows the two apart.
        var back = roundTrip(composition, bundleJson, steps);

        // 8. and 9. The other half of the record, and the two put together. This is what
        //    GET /fhir/Patient/{id}/$everything does, and the only place both stores meet.
        addPatientStep(steps, patientId);
        addAssembledStep(steps, patientId, back);

        return assemble(root, store, steps, composition);
    }

    /**
     * Establishes what was dropped in and pulls the first usable reading out of it, recording the
     * input as the first step.
     */
    private Optional<Reading> recognise(JsonNode root, List<TraceStep> steps) {
        if ("Bundle".equals(root.path("resourceType").asText())) {
            var parsed = pulseObservations.parse(root.toString(), Bundle.class);
            var extraction = extractor.fromBundle(parsed);
            if (extraction.readings().isEmpty()) {
                steps.add(TraceStep.failed("input", messages.get("step.input.bundle"),
                        messages.get("actor.client"), messages.get("explain.input.bundleArrived"),
                        extraction.rejections().isEmpty()
                                ? messages.get("fail.bundle.empty")
                                : messages.get("fail.bundle.unusable",
                                        extraction.rejections().get(0).reason()))
                        .withJson(root));
                return Optional.empty();
            }
            steps.add(TraceStep.of("input", messages.get("step.input.bundle"), messages.get("actor.client"),
                    "fhir", messages.get("explain.input.bundle", "{beats}"), root)
                    .withNote(extraction.rejections().isEmpty() ? null
                            : messages.get("note.bundle.rejected", extraction.rejections().size(),
                                    extraction.rejections().get(0).reason())));
            return Optional.of(extraction.readings().get(0));
        }

        if ("Observation".equals(root.path("resourceType").asText())) {
            var parsed = pulseObservations.parseObservation(root.toString());
            var reading = extractor.reading(parsed);
            if (reading.isEmpty()) {
                steps.add(TraceStep.failed("input", messages.get("step.input.observation"),
                        messages.get("actor.client"), messages.get("explain.input.arrived"),
                        messages.get("fail.observation", extractor.rejectionReason(parsed)))
                        .withJson(root));
                return Optional.empty();
            }
            steps.add(TraceStep.of("input", messages.get("step.input.observation"),
                    messages.get("actor.client"), "fhir", messages.get("explain.input.observation"), root));
            return reading;
        }

        steps.add(TraceStep.failed("input", messages.get("step.input.unknown"), messages.get("actor.client"),
                messages.get("explain.input.raw"), messages.get("fail.unrecognised"))
                .withJson(root));
        return Optional.empty();
    }

    /**
     * Which openEHR record this reading belongs in.
     *
     * <p>A FHIR Observation names a subject; openEHR addresses a record by its EHR id. Nothing in
     * this service translates between the two, because openEHR already answers the question itself:
     * an EHR carries an EHR_STATUS whose subject says who it is about, and that is queryable. The
     * AQL below is the one that really runs.
     */
    private void addEhrStep(List<TraceStep> steps, String patientId) {
        try {
            var ehrId = ehrResolver.ehrIdFor(patientId);
            steps.add(TraceStep.of("ehr", messages.get("step.ehr"), "EHRbase", "openehr",
                            messages.get("explain.ehr"),
                            objectMapper.createObjectNode()
                                    .put("patientId", patientId)
                                    .put("ehrId", ehrId))
                    .withQuery(EhrbaseClient.EHR_BY_SUBJECT_AQL.strip()));
        } catch (Exception e) {
            steps.add(TraceStep.failed("ehr", messages.get("step.ehr"), "EHRbase",
                    messages.get("explain.ehr.short"), messages.get("fail.ehrbase", e.getMessage())));
        }
    }

    /**
     * The two halves put together, which is what {@code $everything} answers with.
     *
     * <p>The readings came back out of openEHR through the mappings; the patient never went in and
     * never came out, because that half lives in the FHIR store. This is the only stage where both
     * are in one document, and nothing in it says which entry came from where.
     */
    private void addAssembledStep(List<TraceStep> steps, String patientId, JsonNode back) {
        if (back == null) {
            return;
        }
        try {
            var bundle = new org.hl7.fhir.r4.model.Bundle();
            bundle.setType(org.hl7.fhir.r4.model.Bundle.BundleType.SEARCHSET);
            bundle.addEntry().setResource(patients.byId(patientId));
            for (JsonNode entry : back.path("entry")) {
                var resource = entry.path("resource");
                if ("Observation".equals(resource.path("resourceType").asText())) {
                    bundle.addEntry().setResource(pulseObservations.parseObservation(resource.toString()));
                }
            }
            bundle.setTotal(bundle.getEntry().size());
            steps.add(TraceStep.of("assembled", messages.get("step.assembled"),
                            messages.get("actor.backend"), "fhir",
                            messages.get("explain.assembled"), tree(pulseObservations.encode(bundle)))
                    .withCall("GET /fhir/Patient/%s/$everything".formatted(patientId), 0)
                    .onTheWayBack());
        } catch (Exception e) {
            steps.add(TraceStep.failed("assembled", messages.get("step.assembled"),
                    messages.get("actor.backend"), messages.get("explain.assembled.short"),
                    messages.get("fail.fhirStore", e.getMessage())).onTheWayBack());
        }
    }

    /**
     * The patient the reading belongs to, as the FHIR store holds them.
     *
     * <p>Every other stage of this pipeline is about one reading travelling between two
     * representations of the same clinical fact. This one is not: it is the administrative half,
     * which never goes through openFHIR and is never an openEHR composition. The Patient's secondary
     * identifier is the whole join — it carries the id of the openEHR record the stages below write
     * to, which is how a client holding one can find the other.
     */
    private void addPatientStep(List<TraceStep> steps, String patientId) {
        try {
            var patient = patients.byId(patientId);
            var ehrId = ehrResolver.ehrIdFor(patientId);
            steps.add(TraceStep.of("patient", messages.get("step.patient"),
                            messages.get("actor.fhirStore"), "fhir",
                            messages.get("explain.patient"),
                            tree(pulseObservations.encode(patient)))
                    .withCall("GET /fhir/Patient/%s".formatted(patientId), 0)
                    .withNote(messages.get("note.patient", patientId, ehrId))
                    .onTheWayBack());
        } catch (Exception e) {
            // The clinical half does not depend on this one, and saying so is more useful than a
            // stage that silently disappears when the FHIR store is down.
            steps.add(TraceStep.failed("patient", messages.get("step.patient"),
                    messages.get("actor.fhirStore"), messages.get("explain.patient.short"),
                    messages.get("fail.fhirStore", e.getMessage())).onTheWayBack());
        }
    }

    private void storeAndQuery(JsonNode composition, List<TraceStep> steps, String ehrId) {
        String uid;
        long started = System.nanoTime();
        try {
            uid = ehrbase.createComposition(ehrId, composition);
        } catch (Exception e) {
            steps.add(TraceStep.failed("stored", messages.get("step.stored"), "EHRbase",
                    messages.get("explain.stored.short"), messages.get("fail.ehrbase", e.getMessage())));
            return;
        }
        long tookMs = (System.nanoTime() - started) / 1_000_000;

        steps.add(TraceStep.of("stored", messages.get("step.stored"), "EHRbase", "openehr",
                messages.get("explain.stored"), objectMapper.createObjectNode()
                        .put("ehrId", ehrId)
                        .put("versionUid", uid))
                .withCall("POST /rest/openehr/v1/ehr/%s/composition".formatted(ehrId), tookMs));

        started = System.nanoTime();
        List<List<JsonNode>> rows;
        try {
            rows = ehrbase.query(AQL, Map.of("ehrId", ehrId));
        } catch (Exception e) {
            steps.add(TraceStep.failed("aql", messages.get("step.aql"), "EHRbase",
                    messages.get("explain.aql.short"), messages.get("fail.aql", e.getMessage()))
                    .onTheWayBack());
            return;
        }
        tookMs = (System.nanoTime() - started) / 1_000_000;

        // The query travels beside the JSON rather than inside it: as a value it would be one long
        // escaped string, and the AQL is the part worth reading.
        var result = objectMapper.createObjectNode();
        var rowsNode = result.putArray("rows");
        rows.stream().limit(5).forEach(row -> {
            var array = rowsNode.addArray();
            row.forEach(array::add);
        });
        result.put("totalRows", rows.size());

        steps.add(TraceStep.of("aql", messages.get("step.aql"), "EHRbase", "openehr",
                messages.get("explain.aql"), result)
                .withCall("POST /rest/openehr/v1/query/aql", tookMs)
                .onTheWayBack()
                .withQuery(AQL)
                .withNote(rows.size() > 5 ? messages.get("note.aql.showing", rows.size()) : null));
    }

    private JsonNode roundTrip(JsonNode composition, String originalBundleJson, List<TraceStep> steps) {
        JsonNode back;
        long started = System.nanoTime();
        try {
            back = openFhir.toFhir(objectMapper.writeValueAsString(composition), properties.templateId());
        } catch (Exception e) {
            steps.add(TraceStep.failed("roundtrip", messages.get("step.roundtrip"), "openFHIR",
                    messages.get("explain.roundtrip.short"),
                    messages.get("fail.roundtrip", e.getMessage())).onTheWayBack());
            return null;
        }
        long tookMs = (System.nanoTime() - started) / 1_000_000;

        var differences = JsonPointers.diff(tree(originalBundleJson), back);
        steps.add(TraceStep.of("roundtrip", messages.get("step.roundtrip"), "openFHIR", "fhir",
                messages.get("explain.roundtrip"), back)
                .withCall("POST /openfhir/tofhir?templateId=%s".formatted(properties.templateId()), tookMs)
                .withDifferences(differences)
                .onTheWayBack());
        return back;
    }

    /** The FHIR ⇄ openEHR correspondences, located in the documents this run actually produced. */
    private List<MappingLink> links(JsonNode composition) {
        if (composition == null) {
            return List.of();
        }
        var links = new ArrayList<MappingLink>();

        JsonPointers.archetypeNodeChild(composition, "at0004", "value").ifPresent(pointer ->
                links.add(link("rate", messages.get("link.rate.label"), "mapped", messages.get("link.rate"),
                        "/entry/0/resource/valueQuantity", pointer, "pulse.model.yaml", "- name: \"rate\"")));

        JsonPointers.archetypeNodeChild(composition, "at0003", "time").ifPresent(pointer ->
                links.add(link("time", messages.get("link.time.label"), "mapped", messages.get("link.time"),
                        "/entry/0/resource/effectiveDateTime", pointer, "pulse.model.yaml", "- name: \"time\"")));

        JsonPointers.archetypeNode(composition, "openEHR-EHR-OBSERVATION.pulse.v2").ifPresent(pointer ->
                links.add(link("code", messages.get("link.code.label"), "selector", messages.get("link.code"),
                        "/entry/0/resource/code", pointer, "pulse.model.yaml", "preprocessor:")));

        JsonPointers.archetypeNode(composition, "openEHR-EHR-OBSERVATION.pulse.v2").ifPresent(pointer ->
                links.add(link("entry", messages.get("link.entry.label"), "mapped", messages.get("link.entry"),
                        "/entry/0", pointer, "heartrate-encounter.model.yaml", "- name: \"pulseParent\"")));

        links.add(link("template", messages.get("link.template.label"), "generated", messages.get("link.template"),
                null, "/archetype_details/template_id/value", "heartrate.context.yaml", "context:"));

        links.add(link("generated", messages.get("link.generated.label"), "generated", messages.get("link.generated"),
                null, "/composer", "heartrate.context.yaml", "context:"));

        return List.copyOf(links);
    }

    private MappingLink link(String id, String label, String kind, String explanation,
            String fhirPointer, String openehrPointer, String file, String anchor) {
        var block = mappings.block(file, anchor).orElse(null);
        return new MappingLink(id, label, kind, explanation, fhirPointer, openehrPointer, file,
                block == null ? null : block[0], block == null ? null : block[1]);
    }

    private Trace assemble(JsonNode root, boolean stored, List<TraceStep> steps, JsonNode composition) {
        return new Trace(kindOf(root), labelOf(root), stored, List.copyOf(steps),
                links(composition), mappings.sources());
    }

    private String kindOf(JsonNode root) {
        return switch (root.path("resourceType").asText("")) {
            case "Bundle" -> "bundle";
            case "Observation" -> "observation";
            default -> "unknown";
        };
    }

    private String labelOf(JsonNode root) {
        return switch (kindOf(root)) {
            case "bundle" -> messages.get("kind.bundle");
            case "observation" -> messages.get("kind.observation");
            default -> messages.get("kind.unknown");
        };
    }

    private JsonNode tree(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("Could not re-read generated JSON", e);
        }
    }

}
