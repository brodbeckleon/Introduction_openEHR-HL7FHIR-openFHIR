package com.example.heartrate.patient;

import static org.assertj.core.api.Assertions.assertThat;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

class PatientResourcesTest {

    private static final String EHR_ID = "7d44b88c-4199-4bad-97dc-d78268e01398";

    /** An update that leaves the identifier out must not cut the patient off from their readings. */
    @Test
    void putsBackALinkTheClientLeftOut() {
        var patient = new Patient();
        patient.addName().setText("Max Mustermann");

        PatientResources.withEhrIdentifier(patient, EHR_ID);

        assertThat(patient.getIdentifier())
                .singleElement()
                .satisfies(identifier -> {
                    assertThat(identifier.getSystem()).isEqualTo(PatientResources.EHR_ID_SYSTEM);
                    assertThat(identifier.getValue()).isEqualTo("urn:uuid:" + EHR_ID);
                    assertThat(identifier.getUse()).isEqualTo(Identifier.IdentifierUse.SECONDARY);
                });
    }

    /** The link is the service's to keep: a client cannot point a patient at someone else's record. */
    @Test
    void replacesALinkTheClientChanged() {
        var patient = new Patient();
        patient.addIdentifier().setSystem(PatientResources.EHR_ID_SYSTEM).setValue("urn:uuid:not-theirs");

        PatientResources.withEhrIdentifier(patient, EHR_ID);

        assertThat(patient.getIdentifier()).extracting(Identifier::getValue).containsExactly("urn:uuid:" + EHR_ID);
    }

    @Test
    void leavesEveryOtherIdentifierAlone() {
        var patient = new Patient();
        patient.addIdentifier().setSystem("urn:oid:2.16.756.5.32").setValue("7560000000001");

        PatientResources.withEhrIdentifier(patient, EHR_ID);

        assertThat(patient.getIdentifier()).extracting(Identifier::getSystem)
                .containsExactly("urn:oid:2.16.756.5.32", PatientResources.EHR_ID_SYSTEM);
    }
}
