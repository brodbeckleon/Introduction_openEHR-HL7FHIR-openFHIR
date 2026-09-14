package com.example.heartrate.web;

import com.example.heartrate.traffic.TrafficEntry;
import com.example.heartrate.traffic.TrafficRecorder;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The standards traffic console: what this service said to openFHIR and EHRbase, and what came back. */
@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    private final TrafficRecorder recorder;

    public TrafficController(TrafficRecorder recorder) {
        this.recorder = recorder;
    }

    /**
     * @param since the highest {@code seq} the caller already has; 0 for everything still buffered
     */
    @GetMapping
    public List<TrafficEntry> since(@RequestParam(defaultValue = "0") long since) {
        return recorder.since(since);
    }

    /** POST rather than DELETE because the dev CORS config allows GET, POST and OPTIONS only. */
    @PostMapping("/clear")
    public void clear() {
        recorder.clear();
    }
}
