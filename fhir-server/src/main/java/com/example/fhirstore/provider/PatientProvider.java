package com.example.fhirstore.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import com.example.fhirstore.store.StoredResource;
import com.example.fhirstore.store.StoredResourceRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

/**
 * Patients: created, read, updated and searched as ordinary FHIR.
 *
 * <p>Nothing here knows about openEHR. The correspondence between a patient and an openEHR record
 * travels as an ordinary identifier on the resource, put there by whoever creates the Patient — this
 * server only stores and finds it. That is what keeps the two halves independent: the FHIR store can
 * be read without EHRbase running, and openEHR keeps its own anchor in {@code EHR_STATUS}.
 */
@Component
public class PatientProvider implements IResourceProvider {

    private final StoredResourceRepository store;
    private final FhirContext fhirContext;

    public PatientProvider(StoredResourceRepository store, FhirContext fhirContext) {
        this.store = store;
        this.fhirContext = fhirContext;
    }

    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }

    @Create
    @Transactional
    public MethodOutcome create(@ResourceParam Patient patient) {
        if (!patient.hasIdElement() || patient.getIdElement().getIdPart() == null) {
            patient.setId(UUID.randomUUID().toString());
        }
        var id = patient.getIdElement().getIdPart();
        patient.setId(id);
        store.save(new StoredResource("Patient", id, encode(patient)));
        return new MethodOutcome(new IdType("Patient", id), true).setResource(patient);
    }

    @Read
    public Patient read(@IdParam IdType id) {
        return store.findByResourceTypeAndResourceId("Patient", id.getIdPart())
                .map(this::decode)
                .orElseThrow(() -> new ResourceNotFoundException(id));
    }

    /**
     * Creates the patient when it is not there yet, which is what makes bootstrapping idempotent:
     * restarting the service that owns the demo patients must not pile up duplicates.
     */
    @Update
    @Transactional
    public MethodOutcome update(@IdParam IdType id, @ResourceParam Patient patient) {
        var idPart = id.getIdPart();
        patient.setId(idPart);
        var existing = store.findByResourceTypeAndResourceId("Patient", idPart);
        if (existing.isPresent()) {
            existing.get().replaceWith(encode(patient));
            store.save(existing.get());
            return new MethodOutcome(new IdType("Patient", idPart), false).setResource(patient);
        }
        store.save(new StoredResource("Patient", idPart, encode(patient)));
        return new MethodOutcome(new IdType("Patient", idPart), true).setResource(patient);
    }

    /**
     * @param identifier optional {@code system|value}; the EHR id is carried as one of these, so
     *     this is how a caller goes from an openEHR record back to the patient it belongs to
     */
    @Search
    public List<Patient> search(@OptionalParam(name = Patient.SP_IDENTIFIER) TokenParam identifier) {
        var all = store.findByResourceTypeOrderByResourceIdAsc("Patient").stream()
                .map(this::decode)
                .toList();
        if (identifier == null) {
            return all;
        }
        var matches = new ArrayList<Patient>();
        for (var patient : all) {
            for (var candidate : patient.getIdentifier()) {
                var systemMatches = identifier.getSystem() == null
                        || identifier.getSystem().equals(candidate.getSystem());
                if (systemMatches && candidate.getValue() != null
                        && candidate.getValue().equals(identifier.getValue())) {
                    matches.add(patient);
                    break;
                }
            }
        }
        return matches;
    }

    private String encode(Patient patient) {
        return fhirContext.newJsonParser().encodeResourceToString(patient);
    }

    private Patient decode(StoredResource stored) {
        return fhirContext.newJsonParser().parseResource(Patient.class, stored.getJson());
    }
}
