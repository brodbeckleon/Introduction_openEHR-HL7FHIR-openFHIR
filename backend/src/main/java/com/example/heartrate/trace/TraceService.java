package com.example.heartrate.trace;

import com.example.heartrate.config.HeartrateProperties;
import com.example.heartrate.config.Messages;
import com.example.heartrate.fhir.HeartRateExtractor;
import com.example.heartrate.fhir.PulseObservations;
import com.example.heartrate.model.Reading;
import com.example.heartrate.openehr.EhrbaseClient;
import com.example.heartrate.service.HeartRateService;
import com.example.heartrate.patient.EhrResolver;
import com.example.heartrate.patient.PatientDirectory;
import com.example.heartrate.openfhir.OpenFhirClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Service;

/**
 * Runs one reading through the whole pipeline and keeps every intermediate representation.
 *
 * <p>{@link com.example.heartrate.service.HeartRateService} does the same work but throws the
 * intermediate forms away, which is right for an import and wrong for learning: the interesting part
 * of this project is what a resting heart rate looks like as FHIR, what openFHIR turns it into, and
 * what openEHR insists on that FHIR never carried. This service exists to make that visible.
 *
 * <p>A trace never writes. Looking is free and repeatable, and a demo whose record fills up with
 * whatever anyone traced is a worse demo — so the way in stops at the record it would have been
 * written to, and the way back reads what is really there. Everything a trace does is a GET.
 */
@Service
public class TraceService {

    /**
     * The window the way back reads, the same one the chart and the default export show. The
     * query, the post-processing and the same-day check are {@link HeartRateService}'s own: the
     * inspector shows what the import does by running the import's parts, not a copy of them.
     */
    private static final int EXPORT_DAYS = 30;

    private final OpenFhirClient openFhir;
    private final HeartRateService heartRate;
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
            HeartRateService heartRate,
            PulseObservations pulseObservations,
            HeartRateExtractor extractor,
            MappingLibrary mappings,
            Messages messages,
            HeartrateProperties properties,
            EhrResolver ehrResolver,
            PatientDirectory patients,
            ObjectMapper objectMapper) {
        this.openFhir = openFhir;
        this.heartRate = heartRate;
        this.pulseObservations = pulseObservations;
        this.extractor = extractor;
        this.mappings = mappings;
        this.messages = messages;
        this.properties = properties;
        this.ehrResolver = ehrResolver;
        this.patients = patients;
        this.objectMapper = objectMapper;
    }

    public Trace trace(String json, String patientId) {
        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("That is not JSON.", e);
        }

        var steps = new ArrayList<TraceStep>();
        // The store is read once for the whole run — the same resource serves both branches and the
        // assembled Bundle, and three identical GETs in the traffic console would only raise the
        // question why.
        var stored = readPatient(patientId);

        var composition = wayIn(root, steps, patientId, stored);

        // A different operation from everything above it, which is why the inspector shows the two
        // apart — and one the input has no say in. A GET reads what the record holds, so the way
        // back is the same whichever input the way in was shown with, and it is there even when the
        // way in stopped at the door.
        var read = wayBack(steps, patientId, stored);

        return assemble(root, steps, composition != null ? composition : read);
    }

    /**
     * The POST: what was dropped in, on its way to the record. Ends at the write it would make.
     *
     * @return the composition openFHIR produced, or {@code null} when the way in stopped before one
     */
    private JsonNode wayIn(JsonNode root, List<TraceStep> steps, String patientId, StoreRead stored) {
        var recognised = recognise(root, steps);
        if (recognised.isEmpty()) {
            return null;
        }
        var reading = recognised.get();

        // 2. The canonical FHIR Observation. Whatever shape the reading arrived in, everything
        //    downstream sees exactly this.
        var observation = pulseObservations.observation(reading, patientId);
        steps.add(TraceStep.of("observation", messages.get("step.observation"), messages.get("actor.backend"),
                "fhir", messages.get("explain.observation"), tree(pulseObservations.encode(observation))));

        // Off to the side: the subject that Observation names lives in the other store, and the
        // reading does not go through it. A branch, so the chain never claims it did.
        addSubjectStep(steps, stored, patientId);

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
            return null;
        }
        long tookMs = (System.nanoTime() - started) / 1_000_000;

        if (composition == null || !composition.has("content") || composition.get("content").isEmpty()) {
            steps.add(TraceStep.failed("composition", messages.get("step.composition"), "openFHIR",
                    messages.get("explain.composition.short"), messages.get("fail.openfhir.empty")));
            return null;
        }

        steps.add(TraceStep.of("composition", messages.get("step.composition"), "openFHIR", "openehr",
                messages.get("explain.composition"), composition)
                .withSummary(messages.get("explain.composition.summary"))
                .withCall("POST /openfhir/toopenehr?templateId=%s&format=canonical"
                        .formatted(properties.templateId()), tookMs));

        // 5. Where the way in ends: the write it would make. Which record it goes to, and whether
        //    that day already holds a composition, are both questions openEHR answers itself and
        //    both are reads — they really run; only the write does not. Neither is a document the
        //    composition became, which is why they are told on this stage rather than given one.
        addWriteStep(steps, observation, composition, patientId);
        return composition;
    }

    /**
     * The GET: what the record holds, on its way out. Starts at EHRbase, as a GET has to.
     *
     * <p>Nothing here comes from the input. The export's own query runs over the export's own
     * window, the newest composition it answers with is the one followed back — the reading the
     * chart shows last — and it goes through the same openFHIR call and the same post-processing
     * as {@code GET /fhir/Observation}. The inspector used to send the composition the way in had
     * just made down this road, with a note admitting it had never been in the store; now the road
     * is walked with what is really there.
     *
     * @return the composition read back, or {@code null} when there was nothing to read
     */
    private JsonNode wayBack(List<TraceStep> steps, String patientId, StoreRead stored) {
        String ehrId;
        try {
            ehrId = ehrResolver.ehrIdFor(patientId);
        } catch (Exception e) {
            steps.add(TraceStep.failed("aql", messages.get("step.aql"), "EHRbase",
                    messages.get("explain.aql.short"), messages.get("fail.aql.noRecord")).onTheWayBack());
            return null;
        }

        long started = System.nanoTime();
        List<List<JsonNode>> rows;
        try {
            rows = heartRate.compositions(EXPORT_DAYS, ehrId);
        } catch (Exception e) {
            steps.add(TraceStep.failed("aql", messages.get("step.aql"), "EHRbase",
                    messages.get("explain.aql.short"), messages.get("fail.aql", e.getMessage()))
                    .onTheWayBack());
            return null;
        }
        long tookMs = (System.nanoTime() - started) / 1_000_000;

        // COMPOSITIONS_AQL orders by the time measured, oldest first: the last usable row is the
        // newest reading, the one the chart ends on.
        JsonNode composition = null;
        String measuredAt = null;
        for (var row : rows) {
            if (!row.isEmpty() && !row.get(0).isNull()) {
                composition = row.get(0);
                measuredAt = row.size() > 1 ? row.get(1).asText(null) : null;
            }
        }
        if (composition == null) {
            steps.add(TraceStep.failed("aql", messages.get("step.aql"), "EHRbase",
                    messages.get("explain.aql.short"), messages.get("fail.aql.empty", EXPORT_DAYS))
                    .onTheWayBack());
            return null;
        }

        // The query travels beside the JSON rather than inside it: as a value it would be one long
        // escaped string, and the AQL is the part worth reading.
        steps.add(TraceStep.of("aql", messages.get("step.aql"), "EHRbase", "openehr",
                messages.get("explain.aql"), composition)
                .withSummary(messages.get("explain.aql.summary"))
                .withCall("POST /rest/openehr/v1/query/aql", tookMs)
                .onTheWayBack()
                .withQuery(HeartRateService.COMPOSITIONS_AQL.strip())
                .withNote(messages.get("note.aql.newest", rows.size(), EXPORT_DAYS, measuredAt)));

        var back = roundTrip(composition, steps);
        var served = addExportStep(steps, back, composition, patientId);

        // The two halves put together, which is what GET /fhir/Patient/{id}/$everything does, with
        // the administrative half hanging off it rather than lying in the line.
        addAssembledStep(steps, stored, served);
        addPatientStep(steps, stored, patientId);
        return composition;
    }

    /**
     * The write a POST would end with, shown but not sent — and the two questions answered first.
     *
     * <p>Which record: a FHIR Observation names a subject, openEHR addresses an EHR by id, and
     * nothing in this service translates between the two, because openEHR answers it itself — an
     * EHR carries an {@code EHR_STATUS} whose subject says who it is about, and that is queryable.
     * {@link EhrResolver} asks once per patient and remembers the answer, so the query is shown
     * here but only runs on the first trace after a start.
     *
     * <p>Whether the day is taken: {@link HeartRateService#record} looks for a composition on the
     * same day and, if there is one, corrects it with a PUT carrying {@code If-Match} — openEHR
     * versions the composition instead of overwriting it, and the old value stays in the record.
     * That query runs every time, here as in the import. What is shown is the request that would
     * follow.
     *
     * <p>Neither answer is a document the composition became, so neither is a stage: the EHR id
     * ends up in the request path, the same-day answer in its method, and both belong to the write.
     */
    private void addWriteStep(
            List<TraceStep> steps, Observation observation, JsonNode composition, String patientId) {
        String ehrId;
        try {
            ehrId = ehrResolver.ehrIdFor(patientId);
        } catch (Exception e) {
            steps.add(TraceStep.failed("write", messages.get("step.write"), messages.get("actor.backend"),
                    messages.get("explain.write.short"), messages.get("fail.ehrbase", e.getMessage())));
            return;
        }

        long started = System.nanoTime();
        var replaces = heartRate.measuredAt(observation).flatMap(at -> heartRate.latestVersionOn(at, ehrId));
        long tookMs = (System.nanoTime() - started) / 1_000_000;

        var path = "/rest/openehr/v1/ehr/" + ehrId + "/composition";
        var request = objectMapper.createObjectNode();
        var lead = messages.get("explain.write.lead", patientId);
        String explanation;
        String summary;
        if (replaces.isPresent()) {
            var preceding = replaces.get();
            request.put("method", "PUT");
            request.put("path", path + "/" + HeartRateService.versionedObjectUid(preceding));
            var headers = request.putObject("headers");
            headers.put("If-Match", preceding);
            headers.put("Prefer", "return=minimal");
            request.put("replaces", preceding);
            explanation = messages.get("explain.write.put", lead, preceding);
            summary = messages.get("explain.write.summary.put");
        } else {
            request.put("method", "POST");
            request.put("path", path);
            request.putObject("headers").put("Prefer", "return=minimal");
            explanation = messages.get("explain.write.post", lead);
            summary = messages.get("explain.write.summary.post");
        }
        request.set("body", composition);

        // Two queries, one block: AQL takes -- comments, so each can say what it is for. The
        // call label names the one that was timed, because a stage titled "create" showing a
        // query call as its request would leave two POSTs on screen and no way to tell them apart.
        var queries = "-- " + messages.get("query.ehr") + "\n" + EhrbaseClient.EHR_BY_SUBJECT_AQL.strip()
                + "\n\n-- " + messages.get("query.sameDay") + "\n" + HeartRateService.SAME_DAY_AQL.strip();

        steps.add(TraceStep.of("write", messages.get("step.write"), messages.get("actor.backend"), "openehr",
                        explanation, request)
                .withSummary(summary)
                .withCall("POST /rest/openehr/v1/query/aql \u2014 " + messages.get("call.sameDay"), tookMs)
                .withQuery(queries)
                .withNote(messages.get("note.write.dry")));
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
                    .withSummary(messages.get("explain.input.bundle.summary"))
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
     * What {@code GET /fhir/Observation} actually serves, as distinct from what openFHIR answered.
     *
     * <p>The mapping engine hands back Observations with ids of its own and no subject; the backend
     * replaces the id with the composition's uid, adds the version, and names the patient — the
     * same {@link HeartRateService#observationsOf} the export runs. The round-trip differences are
     * measured here, against the document a client receives, so a field the backend fills back in
     * is not reported as lost.
     *
     * @return the served Observations, for the {@code $everything} stage to join with the patient
     */
    private List<Observation> addExportStep(
            List<TraceStep> steps, JsonNode back, JsonNode composition, String patientId) {
        if (back == null) {
            return null;
        }
        var observations = heartRate.observationsOf(back, composition, patientId);
        var served = heartRate.searchset(observations);
        steps.add(TraceStep.of("export", messages.get("step.export"), messages.get("actor.backend"), "fhir",
                        messages.get("explain.export"), tree(pulseObservations.encode(served)))
                .withSummary(messages.get("explain.export.summary"))
                .withDifferences(roundTripDifferences(observations, patientId))
                .onTheWayBack());
        return observations;
    }

    /**
     * What the trip through openEHR did to the reading.
     *
     * <p>What went in is not on hand — the reading came out of the store — but it is
     * reconstructible: every reading is written in the one canonical form, and that form is fixed
     * by the time, the rate and the patient, all of which the served Observation still carries. The
     * time is copied as the text it came back as, so a difference in how two runtimes spell the
     * same instant cannot masquerade as a loss. Compared as plain collections, the shape a reading
     * arrives in: the searchset's type and total are the export's own and would only be noise.
     */
    private List<RoundTripDifference> roundTripDifferences(List<Observation> served, String patientId) {
        if (served.isEmpty()) {
            return List.of();
        }
        try {
            var first = served.get(0);
            var sent = pulseObservations.observation(pulseObservations.toReading(first), patientId);
            sent.setEffective(first.getEffectiveDateTimeType().copy());
            return JsonPointers.diff(
                    tree(pulseObservations.encode(pulseObservations.bundle(List.of(sent)))),
                    tree(pulseObservations.encode(pulseObservations.bundle(served))));
        } catch (Exception e) {
            // A served Observation without a time or a value has nothing canonical to stand beside.
            return List.of();
        }
    }

    /**
     * The two halves put together, which is what {@code $everything} answers with.
     *
     * <p>The readings came back out of openEHR through the mappings; the patient never went in and
     * never came out, because that half lives in the FHIR store. This is the only stage where both
     * are in one document, and nothing in it says which entry came from where. No call is recorded:
     * the backend assembles this, no server produced it.
     */
    private void addAssembledStep(List<TraceStep> steps, StoreRead stored, List<Observation> served) {
        if (served == null) {
            return;
        }
        try {
            var bundle = new Bundle();
            bundle.setType(Bundle.BundleType.SEARCHSET);
            bundle.addEntry().setResource(stored.patient());
            served.forEach(observation -> bundle.addEntry().setResource(observation));
            bundle.setTotal(bundle.getEntry().size());
            steps.add(TraceStep.of("assembled", messages.get("step.assembled"),
                            messages.get("actor.backend"), "fhir",
                            messages.get("explain.assembled"), tree(pulseObservations.encode(bundle)))
                    .withSummary(messages.get("explain.assembled.summary"))
                    .onTheWayBack());
        } catch (Exception e) {
            steps.add(TraceStep.failed("assembled", messages.get("step.assembled"),
                    messages.get("actor.backend"), messages.get("explain.assembled.short"),
                    messages.get("fail.fhirStore", e.getMessage())).onTheWayBack());
        }
    }

    /**
     * The subject the reading names, off to the side of the way in.
     *
     * <p>A branch rather than a stage, because nothing downstream is built from it: the Observation
     * carries a reference, the import writes that reference and never follows it. Drawn in the chain
     * it would say the Bundle was made out of a Patient.
     */
    private void addSubjectStep(List<TraceStep> steps, StoreRead stored, String patientId) {
        // No call on this one: the import never makes it, and a call label with a timing would say
        // it did. The note explains what the lookup is for.
        steps.add(fhirStoreStep("subject", messages.get("step.subject"), messages.get("explain.subject"),
                messages.get("explain.subject.summary"), messages.get("explain.subject.short"), "note.subject",
                stored, patientId, false));
    }

    /**
     * The patient the FHIR store holds, off to the side of the way back.
     *
     * <p>Every other stage is about one reading travelling between two representations of the same
     * clinical fact. This one is not: it is the administrative half, which never goes through
     * openFHIR and is never an openEHR composition. It joins the line at the Bundle above it and
     * nowhere else, which is exactly what a branch says and a chain does not.
     */
    private void addPatientStep(List<TraceStep> steps, StoreRead stored, String patientId) {
        // This read is the one $everything makes, so it is shown as what it is, timed: not a call to
        // another server any more, but a query against this service's own database.
        steps.add(fhirStoreStep("patient", messages.get("step.patient"), messages.get("explain.patient"),
                        messages.get("explain.patient.summary"), messages.get("explain.patient.short"),
                        "note.patient", stored, patientId, true)
                .onTheWayBack());
    }

    /** One read of the FHIR store: what it answered, how long it took, or why it did not. */
    private record StoreRead(Patient patient, long tookMs, Exception failure) {}

    private StoreRead readPatient(String patientId) {
        long started = System.nanoTime();
        try {
            var patient = patients.byId(patientId);
            return new StoreRead(patient, (System.nanoTime() - started) / 1_000_000, null);
        } catch (Exception e) {
            return new StoreRead(null, (System.nanoTime() - started) / 1_000_000, e);
        }
    }

    /**
     * One read of the FHIR store, as a branch.
     *
     * <p>Both halves ask it the same question and get the same resource; what differs is what the
     * answer is for, so the text is passed in. A failure is a step rather than a gap: the clinical
     * half does not depend on this one, and saying so is more useful than a stage that silently
     * disappears when the FHIR store is down.
     *
     * @param showCall whether the operation being traced makes this read — a GET does, a POST does
     *     not, and the label must not claim otherwise
     */
    private TraceStep fhirStoreStep(
            String id, String title, String explanation, String summary, String shortExplanation,
            String noteKey, StoreRead stored, String patientId, boolean showCall) {
        try {
            if (stored.failure() != null) {
                throw stored.failure();
            }
            var ehrId = ehrResolver.ehrIdFor(patientId);
            var step = TraceStep.of(id, title, messages.get("actor.fhirStore"), "fhir", explanation,
                            tree(pulseObservations.encode(stored.patient())))
                    .withSummary(summary)
                    .withNote(messages.get(noteKey, patientId, ehrId))
                    .asBranch();
            return showCall
                    ? step.withCall("SELECT json FROM stored_resource WHERE resource_id = '%s'"
                            .formatted(patientId), stored.tookMs())
                    : step;
        } catch (Exception e) {
            return TraceStep.failed(id, title, messages.get("actor.fhirStore"), shortExplanation,
                            messages.get("fail.fhirStore", e.getMessage()))
                    .asBranch();
        }
    }

    /** openFHIR's answer, as it answered — what the backend then does to it is the next stage. */
    private JsonNode roundTrip(JsonNode composition, List<TraceStep> steps) {
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

        steps.add(TraceStep.of("roundtrip", messages.get("step.roundtrip"), "openFHIR", "fhir",
                messages.get("explain.roundtrip"), back)
                .withSummary(messages.get("explain.roundtrip.summary"))
                .withCall("POST /openfhir/tofhir?templateId=%s".formatted(properties.templateId()), tookMs)
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

    private Trace assemble(JsonNode root, List<TraceStep> steps, JsonNode composition) {
        return new Trace(kindOf(root), labelOf(root), List.copyOf(steps),
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
