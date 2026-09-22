package com.example.heartrate.patient;

import com.example.heartrate.openehr.EhrbaseClient;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Answers which EHR a patient's readings belong in.
 *
 * <p>This replaces the fixed {@code heartrate.ehr-id} the demo used to carry. The id is no longer
 * configuration: it is looked up from {@code EHR_STATUS.subject} and created on first use, which is
 * what lets the app hold more than one record without being told about any of them in advance.
 *
 * <p>The cache is a courtesy, not a correctness requirement — every entry is reproducible from
 * EHRbase, so losing it on restart costs one query per patient.
 */
@Component
public class EhrResolver {

    private final EhrbaseClient ehrbase;
    private final Map<String, String> known = new ConcurrentHashMap<>();

    public EhrResolver(EhrbaseClient ehrbase) {
        this.ehrbase = ehrbase;
    }

    public String ehrIdFor(String patientId) {
        var cached = known.get(patientId);
        if (cached != null) {
            return cached;
        }
        // Deliberately not computeIfAbsent: this blocks on EHRbase, and holding the map's bin lock
        // across a network call would stall every other patient resolving at the same time.
        var ehrId = ehrbase.findEhrByPatient(patientId)
                .orElseGet(() -> ehrbase.createEhr(UUID.randomUUID().toString(), patientId));
        known.put(patientId, ehrId);
        return ehrId;
    }
}
