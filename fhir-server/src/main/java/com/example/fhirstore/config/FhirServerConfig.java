package com.example.fhirstore.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.List;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mounts the FHIR servlet at {@code /fhir/*} and hands it every resource provider in the context.
 */
@Configuration
public class FhirServerConfig {

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    public ServletRegistrationBean<RestfulServer> fhirServlet(
            FhirContext fhirContext, List<IResourceProvider> providers) {
        var server = new RestfulServer(fhirContext);
        server.setResourceProviders(providers);
        // Without this the server advertises capabilities at a path that does not match where the
        // servlet is actually mounted, and every generated URL in a Bundle points somewhere wrong.
        server.setServerAddressStrategy(
                new ca.uhn.fhir.rest.server.IncomingRequestAddressStrategy());
        var registration = new ServletRegistrationBean<>(server, "/fhir/*");
        registration.setName("fhir");
        return registration;
    }
}
