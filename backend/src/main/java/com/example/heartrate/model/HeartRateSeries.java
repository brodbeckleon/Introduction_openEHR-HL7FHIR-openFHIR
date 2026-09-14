package com.example.heartrate.model;

import java.time.LocalDate;
import java.util.List;

/** What the Svelte frontend renders. */
public record HeartRateSeries(LocalDate from, LocalDate to, List<DailyRestingHeartRate> days) {}
