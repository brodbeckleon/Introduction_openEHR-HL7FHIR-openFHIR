package com.example.heartrate.web;

import com.example.heartrate.history.DayHistory;
import com.example.heartrate.history.HistoryService;
import com.example.heartrate.model.HeartRateSeries;
import com.example.heartrate.service.HeartRateService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The API the Svelte frontend uses. Data only ever arrives through the FHIR endpoints. */
@RestController
@RequestMapping("/api")
public class HeartRateController {

    private final HeartRateService service;
    private final HistoryService history;

    public HeartRateController(HeartRateService service, HistoryService history) {
        this.service = service;
        this.history = history;
    }

    @GetMapping("/heart-rate")
    public HeartRateSeries series(@RequestParam(defaultValue = "30") int days) {
        return service.series(Math.clamp(days, 1, 365));
    }

    /**
     * Every version of what is recorded for one day, including the values it used to hold.
     *
     * <p>Not a FHIR endpoint: FHIR versioning is optional and server-specific, while this is the
     * openEHR record's own revision history.
     */
    @GetMapping("/history")
    public DayHistory history(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return history.of(date);
    }
}
