package com.example.heartrate.web;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What this build can do.
 *
 * <p>The frontend runs in a container and the backend runs on the host, so the two drift apart the
 * moment someone rebuilds one and forgets the other — and the symptom is indirect: corrections that
 * do not appear, tabs that stay empty. The page asks for this on load; a 404 means the backend
 * predates the endpoint and is therefore older than the page, which is worth saying out loud.
 */
@RestController
@RequestMapping("/api")
public class AboutController {

    /** Named so a page can check for the one it needs rather than compare version numbers. */
    private static final List<String> CAPABILITIES =
            List.of("trace", "traffic", "composition-versioning", "manual-entry");

    @GetMapping("/about")
    public Map<String, Object> about() {
        return Map.of("application", "heartrate-monitor", "capabilities", CAPABILITIES);
    }
}
