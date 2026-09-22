package com.example.heartrate.config;

import ca.uhn.fhir.context.FhirContext;
import com.example.heartrate.traffic.TrafficInterceptor;
import com.example.heartrate.traffic.TrafficRecorder;
import java.time.Duration;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    /** HAPI's FhirContext is expensive to build and thread safe, so it lives as a singleton. */
    @Bean
    FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    RestClientCustomizer restClientCustomizer() {
        return builder -> builder.requestFactory(requestFactory());
    }

    /**
     * Buffering so response bodies can be read twice: once by the traffic console, once by the code
     * that asked for them.
     */
    private ClientHttpRequestFactory requestFactory() {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        return new BufferingClientHttpRequestFactory(factory);
    }

    @Bean
    RestClient ehrbaseRestClient(
            RestClient.Builder builder, HeartrateProperties properties, TrafficRecorder recorder) {
        var ehrbase = properties.ehrbase();
        return builder.clone()
                .baseUrl(ehrbase.baseUrl())
                .defaultHeaders(headers -> headers.setBasicAuth(ehrbase.username(), ehrbase.password()))
                .requestInterceptor(new TrafficInterceptor(recorder, "EHRbase"))
                .build();
    }

    /**
     * The traffic interceptor is deliberately attached here too: the console then shows all three
     * systems this app talks to, and the FHIR store's calls sit beside EHRbase's, which is the
     * clearest way to see which half of a record came from where.
     */
    @Bean
    RestClient fhirStoreRestClient(
            RestClient.Builder builder, HeartrateProperties properties, TrafficRecorder recorder) {
        return builder.clone()
                .baseUrl(properties.fhirStore().baseUrl())
                .requestInterceptor(new TrafficInterceptor(recorder, "FHIR store"))
                .build();
    }

    @Bean
    RestClient openFhirRestClient(
            RestClient.Builder builder, HeartrateProperties properties, TrafficRecorder recorder) {
        return builder.clone()
                .baseUrl(properties.openfhir().baseUrl())
                .requestInterceptor(new TrafficInterceptor(recorder, "openFHIR"))
                .build();
    }
}
