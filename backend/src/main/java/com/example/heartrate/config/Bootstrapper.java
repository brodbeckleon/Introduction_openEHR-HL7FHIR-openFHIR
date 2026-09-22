package com.example.heartrate.config;

import com.example.heartrate.openehr.EhrbaseClient;
import com.example.heartrate.patient.EhrResolver;
import com.example.heartrate.patient.PatientDirectory;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Makes sure EHRbase knows the operational template and has an EHR for every configured patient
 * before the first reading arrives. openFHIR picks up the same template from its bootstrap directory
 * on container start.
 *
 * <p>Resolving each patient here is not strictly necessary — the first request would create the EHR
 * anyway — but it means the AQL playground has something to query before anyone has recorded
 * anything.
 */
@Component
public class Bootstrapper implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(Bootstrapper.class);

    private final EhrbaseClient ehrbase;
    private final PatientDirectory directory;
    private final EhrResolver ehrResolver;

    public Bootstrapper(EhrbaseClient ehrbase, PatientDirectory directory, EhrResolver ehrResolver) {
        this.ehrbase = ehrbase;
        this.directory = directory;
        this.ehrResolver = ehrResolver;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            var opt = new ClassPathResource("heartrate_monitor.opt")
                    .getContentAsString(StandardCharsets.UTF_8);
            ehrbase.uploadTemplate(opt);
            for (var patient : directory.all()) {
                log.info("Patient {} uses EHR {}", patient.id(), ehrResolver.ehrIdFor(patient.id()));
            }
        } catch (Exception e) {
            log.error("Could not prepare EHRbase — is `docker compose up` running? ({})", e.getMessage());
        }
    }
}
