package com.example.heartrate.config;

import com.example.heartrate.openehr.EhrbaseClient;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Makes sure EHRbase knows the operational template and has the demo EHR before the first reading
 * arrives. openFHIR picks up the same template from its bootstrap directory on container start.
 */
@Component
public class Bootstrapper implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(Bootstrapper.class);

    private final EhrbaseClient ehrbase;
    private final HeartrateProperties properties;

    public Bootstrapper(EhrbaseClient ehrbase, HeartrateProperties properties) {
        this.ehrbase = ehrbase;
        this.properties = properties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            var opt = new ClassPathResource("heartrate_monitor.opt")
                    .getContentAsString(StandardCharsets.UTF_8);
            ehrbase.uploadTemplate(opt);
            ehrbase.ensureEhr(properties.ehrId());
        } catch (Exception e) {
            log.error("Could not prepare EHRbase — is `docker compose up` running? ({})", e.getMessage());
        }
    }
}
