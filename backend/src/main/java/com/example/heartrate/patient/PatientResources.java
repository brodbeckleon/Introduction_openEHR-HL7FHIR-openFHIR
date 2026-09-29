package com.example.heartrate.patient;

import com.example.heartrate.config.HeartrateProperties;
import java.time.ZoneOffset;
import java.util.Date;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

/**
 * Turns a configured patient into a FHIR Patient resource, and keeps the link to its record intact.
 *
 * <p>The projection is what the FHIR store is seeded with, and what the directory falls back to
 * when that store cannot be reached. Either way {@code Observation.subject} resolves to a Patient
 * that exists.
 *
 * <p>The EHR id travels as a secondary identifier. A FHIR client that has the Patient can therefore
 * find its openEHR record without this service explaining the correspondence — the same bridge the
 * vaccination reference server builds between its two stores.
 */
@Component
public class PatientResources {

    /** Namespace for the EHR id carried on the Patient; matches EHR_STATUS.subject's namespace. */
    public static final String EHR_ID_SYSTEM = "urn:heartrate-monitor:ehr-id";

    public Patient toFhir(HeartrateProperties.Patient patient, String ehrId) {
        var resource = new Patient();
        resource.setId(patient.id());
        withEhrIdentifier(resource, ehrId);
        if (patient.name() != null) {
            resource.addName().setText(patient.name());
        }
        if (patient.birthDate() != null) {
            resource.setBirthDate(Date.from(patient.birthDate().atStartOfDay(ZoneOffset.UTC).toInstant()));
        }
        if (patient.gender() != null) {
            resource.setGender(Enumerations.AdministrativeGender.fromCode(patient.gender()));
        }
        var address = patient.address();
        if (address != null) {
            resource.addAddress()
                    .addLine(address.line())
                    .setPostalCode(address.postalCode())
                    .setCity(address.city())
                    .setCountry(address.country());
        }
        return resource;
    }

    /**
     * Makes sure the Patient names its openEHR record, and names the right one.
     *
     * <p>A FHIR update replaces the whole resource, so a client editing an address can drop the
     * identifier without meaning to — and with it the only way from this Patient to its readings.
     * The link is this service's to keep, not the client's: whatever the client sent for it is
     * replaced by the EHR id openEHR actually holds, and every other identifier is left alone.
     */
    public static Patient withEhrIdentifier(Patient patient, String ehrId) {
        patient.getIdentifier().removeIf(identifier -> EHR_ID_SYSTEM.equals(identifier.getSystem()));
        patient.addIdentifier()
                .setSystem(EHR_ID_SYSTEM)
                .setValue("urn:uuid:" + ehrId)
                .setUse(Identifier.IdentifierUse.SECONDARY);
        return patient;
    }
}
