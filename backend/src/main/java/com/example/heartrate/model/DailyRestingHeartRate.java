package com.example.heartrate.model;

import java.time.LocalDate;

/** One day's resting heart rate, as stored in the CDR. */
public record DailyRestingHeartRate(LocalDate date, double resting) {}
