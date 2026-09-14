package com.example.heartrate.trace;

import com.example.heartrate.config.HeartrateProperties;
import com.example.heartrate.config.Messages;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Serves the FHIR Connect mappings to the UI and finds the rule behind a given correspondence.
 *
 * <p>The files are read fresh on every request from {@code openfhir-bootstrap/} when that directory
 * is reachable, so an edit shows up in the browser as soon as openFHIR has been re-bootstrapped.
 * Falling back to the copy on the classpath keeps this working when the service runs from a jar.
 */
@Component
public class MappingLibrary {

    private static final Logger log = LoggerFactory.getLogger(MappingLibrary.class);

    /** File name to the message key of the one-line description the UI puts above it. */
    private static final Map<String, String> FILES = new LinkedHashMap<>();

    static {
        FILES.put("heartrate.context.yaml", "mapping.context");
        FILES.put("heartrate-encounter.model.yaml", "mapping.encounter");
        FILES.put("pulse.model.yaml", "mapping.pulse");
    }

    private final Path directory;
    private final Messages messages;

    public MappingLibrary(HeartrateProperties properties, Messages messages) {
        this.directory = Path.of(properties.mappingsDir());
        this.messages = messages;
    }

    /**
     * Overwrites one mapping file.
     *
     * <p>Only the files this library knows are writable, which also rules out escaping the directory
     * through the name. The content is not validated here: a mapping that openFHIR rejects is a
     * result worth seeing, not an error to prevent.
     */
    public void write(String file, String content) {
        if (!FILES.containsKey(file)) {
            throw new IllegalArgumentException("Not a mapping of this application: " + file);
        }
        var path = directory.resolve(file);
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
            log.info("Wrote mapping {}", path);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not write %s. Is %s mounted writable?".formatted(path, directory), e);
        }
    }

    /**
     * Puts one mapping back the way it was shipped.
     *
     * <p>The copy on the classpath is the baseline — it was taken from this directory when the
     * application was built, so it is the last version that was not edited here.
     */
    public void reset(String file) {
        if (!FILES.containsKey(file)) {
            throw new IllegalArgumentException("Not a mapping of this application: " + file);
        }
        try {
            write(file, new ClassPathResource("mappings/" + file).getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("No shipped copy of " + file + " to restore", e);
        }
    }

    /** True when the file on disk still matches the copy the application was built with. */
    public boolean isPristine(String file) {
        try {
            var shipped = new ClassPathResource("mappings/" + file).getContentAsString(StandardCharsets.UTF_8);
            return read(file).map(shipped::equals).orElse(false);
        } catch (IOException e) {
            return true;
        }
    }

    /** Every mapping file, in the order they are worth reading. */
    public List<MappingSource> sources() {
        return FILES.entrySet().stream()
                .map(entry -> read(entry.getKey())
                        .map(content -> new MappingSource(entry.getKey(), messages.get(entry.getValue()),
                                content, !isPristine(entry.getKey()))))
                .flatMap(Optional::stream)
                .toList();
    }

    /**
     * Every rule in every mapping file, in the order they are written.
     *
     * <p>Read from the YAML rather than hardcoded, so editing a mapping changes the list — which is
     * the point: the rules are the mapping, and a list that could disagree with the file would be
     * worse than none.
     */
    public List<MappingRule> rules() {
        var rules = new java.util.ArrayList<MappingRule>();
        FILES.keySet().forEach(file -> read(file).ifPresent(content -> rules.addAll(rulesIn(file, content))));
        return List.copyOf(rules);
    }

    private static final java.util.regex.Pattern RULE =
            java.util.regex.Pattern.compile("^(\\s*)- name: \"([^\"]+)\"");

    private static List<MappingRule> rulesIn(String file, String content) {
        var lines = content.split("\n", -1);
        var rules = new java.util.ArrayList<MappingRule>();

        for (int i = 0; i < lines.length; i++) {
            var start = RULE.matcher(lines[i]);
            if (!start.find()) {
                continue;
            }
            int indent = start.group(1).length();
            int end = endOfBlock(lines, i, indent);

            String fhir = null;
            String openehr = null;
            String constantPath = null;
            String value = null;
            boolean withBlock = false;

            for (int j = i + 1; j <= end; j++) {
                var line = lines[j];
                var trimmed = line.strip();
                if (indentOf(line) <= indent && !trimmed.isEmpty()) {
                    break;
                }
                if (trimmed.equals("with:")) {
                    withBlock = true;
                } else if (withBlock && trimmed.startsWith("fhir:") && fhir == null) {
                    fhir = valueOf(trimmed);
                } else if (withBlock && trimmed.startsWith("openehr:") && openehr == null) {
                    openehr = valueOf(trimmed);
                } else if (trimmed.startsWith("- path:") && constantPath == null) {
                    constantPath = valueOf(trimmed.substring(2));
                } else if (trimmed.startsWith("value:") && value == null) {
                    value = valueOf(trimmed);
                }
            }

            if (fhir != null && openehr != null) {
                rules.add(new MappingRule(file, start.group(2), "correspondence", indent / 2,
                        fhir, openehr, null, i + 1, end + 1));
            } else if (constantPath != null) {
                rules.add(new MappingRule(file, start.group(2), "constant", indent / 2,
                        constantPath, null, value, i + 1, end + 1));
            }
        }
        return rules;
    }

    /** The last line belonging to a rule: everything indented deeper than its own {@code - name:}. */
    private static int endOfBlock(String[] lines, int start, int indent) {
        int end = start;
        for (int i = start + 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            if (indentOf(lines[i]) <= indent) {
                break;
            }
            end = i;
        }
        return end;
    }

    /** {@code fhir: "$resource.value"} -> {@code $resource.value}. */
    private static String valueOf(String line) {
        var text = line.substring(line.indexOf(':') + 1).strip();
        if (text.length() >= 2 && text.startsWith("\"") && text.endsWith("\"")) {
            text = text.substring(1, text.length() - 1);
        }
        return text.isEmpty() ? null : text;
    }

    /**
     * The line range of the block introduced by {@code anchor}, 1-based and inclusive.
     *
     * <p>A block runs from its anchor line to the last line indented further than it — which is
     * exactly how YAML nests, so no parser is needed to point at a rule.
     */
    public Optional<int[]> block(String file, String anchor) {
        return read(file).flatMap(content -> blockIn(content, anchor));
    }

    private Optional<int[]> blockIn(String content, String anchor) {
        var lines = content.split("\n", -1);
        int start = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].strip().startsWith(anchor)) {
                start = i;
                break;
            }
        }
        if (start < 0) {
            return Optional.empty();
        }

        int indent = indentOf(lines[start]);
        int end = start;
        // Blank lines inside a block are skipped rather than ended on, but only lines indented deeper
        // than the anchor extend it — so a trailing blank line or the comment introducing the next
        // rule is not highlighted with this one.
        for (int i = start + 1; i < lines.length; i++) {
            if (lines[i].isBlank()) {
                continue;
            }
            if (indentOf(lines[i]) <= indent) {
                break;
            }
            end = i;
        }
        return Optional.of(new int[] {start + 1, end + 1});
    }

    private static int indentOf(String line) {
        int i = 0;
        while (i < line.length() && line.charAt(i) == ' ') {
            i++;
        }
        return i;
    }

    private Optional<String> read(String file) {
        var path = directory.resolve(file);
        if (Files.isReadable(path)) {
            try {
                return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
            } catch (IOException e) {
                log.warn("Could not read mapping {}: {}", path, e.getMessage());
            }
        }
        try {
            return Optional.of(new ClassPathResource("mappings/" + file)
                    .getContentAsString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.warn("Mapping {} is neither under {} nor on the classpath", file, directory);
            return Optional.empty();
        }
    }
}
