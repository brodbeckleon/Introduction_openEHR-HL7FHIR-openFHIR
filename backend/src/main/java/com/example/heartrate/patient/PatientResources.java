package com.example.heartrate.patient;

import com.example.heartrate.config.HeartrateProperties;
import java.time.ZoneOffset;
import java.util.Date;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.stereotype.Component;

/**
 * Projects a configured patient into a FHIR Patient resource.
 *
 * <p>Nothing is stored: the resource is built on every read from the directory entry and the EHR id
 * openEHR answers with. That is the point of this step — {@code Observation.subject} used to point
 * at a {@code Patient/demo-patient} that existed nowhere, and now it resolves.
 *
 * <p>The EHR id travels as a secondary identifier. A FHIR client that has the Patient can therefore
 * find its openEHR record without this service explaining the correspondence, which is the same
 * bridge the vaccination reference server builds — there between two stores, here between a
 * projection and one store.
 */
@Component
public class PatientResources {

    /** Namespace for the EHR id carried on the Patient; matches EHR_STATUS.subject's namespace. */
    public static final String EHR_ID_SYSTEM = "urn:heartrate-monitor:ehr-id";

    public Patient toFhir(HeartrateProperties.Patient patient, String ehrId) {
        var resource = new Patient();
        resource.setId(patient.id());
        resource.addIdentifier()
                .setSystem(EHR_ID_SYSTEM)
                .setValue("urn:uuid:" + ehrId)
                .setUse(Identifier.IdentifierUse.SECONDARY);
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
}
