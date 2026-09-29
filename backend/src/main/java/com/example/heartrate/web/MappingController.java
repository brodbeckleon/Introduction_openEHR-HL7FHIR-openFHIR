package com.example.heartrate.web;

import com.example.heartrate.openfhir.OpenFhirLoader;
import com.example.heartrate.trace.MappingLibrary;
import com.example.heartrate.trace.MappingRule;
import com.example.heartrate.trace.MappingSource;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Editing the FHIR Connect mappings from the browser.
 *
 * <p>The mappings are declarative and openFHIR takes a new version of one over its REST API, so
 * changing how FHIR and openEHR correspond needs no rebuild and no restart. That is worth doing
 * rather than reading about: breaking a mapping on purpose and watching where it fails teaches more
 * about what a mapping engine does than the working version ever will.
 *
 * <p>These write to the files in {@code openfhir-bootstrap/} — the real ones, the same ones this
 * service hands openFHIR on every start. Every file can be put back with {@code reset}.
 */
@RestController
@RequestMapping("/api/mappings")
public class MappingController {

    /**
     * What one save did.
     *
     * @param applied whether openFHIR accepted the mapping after the file was written
     * @param detail openFHIR's complaint when it did not
     */
    public record SaveResult(boolean applied, String detail, List<MappingSource> mappings) {}

    private final MappingLibrary mappings;
    private final OpenFhirLoader openFhir;

    public MappingController(MappingLibrary mappings, OpenFhirLoader openFhir) {
        this.mappings = mappings;
        this.openFhir = openFhir;
    }

    @GetMapping
    public List<MappingSource> all() {
        return mappings.sources();
    }

    /** Every rule the mappings declare: what each connects, and where it is written. */
    @GetMapping("/rules")
    public List<MappingRule> rules() {
        return mappings.rules();
    }

    /**
     * Writes one mapping and hands openFHIR the new version of it.
     *
     * <p>A mapping openFHIR rejects still gets written: the file on disk is what you typed, the
     * engine keeps running with the version it had, and the answer says what went wrong. Refusing
     * the save would hide exactly the lesson the editor exists for.
     */
    @PostMapping(value = "/{file}", consumes = MediaType.TEXT_PLAIN_VALUE)
    public SaveResult save(@PathVariable String file, @RequestBody String content) {
        mappings.write(file, content);
        return upload(file);
    }

    /** Restores the copy the application was built with, and hands openFHIR that. */
    @PostMapping("/{file}/reset")
    public SaveResult reset(@PathVariable String file) {
        mappings.reset(file);
        return upload(file);
    }

    private SaveResult upload(String file) {
        try {
            var outcome = openFhir.load(file);
            return new SaveResult(outcome.applied(), outcome.detail(), mappings.sources());
        } catch (Exception e) {
            return new SaveResult(false, rootCause(e), mappings.sources());
        }
    }

    /** openFHIR's own message is the useful part; the wrapping exception chain is not. */
    private static String rootCause(Throwable error) {
        var cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        var message = cause.getMessage();
        return message == null || message.isBlank() ? error.toString() : message;
    }
}
