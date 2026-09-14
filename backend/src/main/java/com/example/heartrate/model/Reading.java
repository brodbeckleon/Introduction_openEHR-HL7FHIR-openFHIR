package com.example.heartrate.model;

import java.time.OffsetDateTime;

/** A reading as it comes back out of the openEHR CDR. */
public record Reading(OffsetDateTime measuredAt, double beatsPerMinute) {}
