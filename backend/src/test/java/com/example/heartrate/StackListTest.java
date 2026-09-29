package com.example.heartrate;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * The Build your own tab names exact versions for people to rebuild with, and a version list that
 * disagrees with what actually runs is worse than none: someone copies it. So the list in
 * {@code frontend/src/lib/stack.ts} is held against the files that really decide — the compose
 * file and the Gradle build.
 *
 * <p>Paths are relative to {@code backend/}, where Gradle runs the tests, as in the mapping tests.
 */
class StackListTest {

    private static final Path ROOT = Path.of("..");

    private final String stack = read("frontend/src/lib/stack.ts");

    @Test
    void listsEveryImageTheComposeFilePulls() {
        var images = services().values().stream()
                .map(service -> (String) service.get("image"))
                .filter(Objects::nonNull)
                .toList();

        assertThat(images).isNotEmpty();
        assertThat(images).allSatisfy(image -> assertThat(stack).contains("'" + image + "'"));
    }

    @Test
    void namesTheVersionsTheGradleBuildPins() {
        var gradle = read("backend/build.gradle");
        var springBoot = find(gradle, "id 'org.springframework.boot' version '([^']+)'");
        var java = find(gradle, "JavaLanguageVersion\\.of\\((\\d+)\\)");
        var hapi = find(gradle, "hapiVersion = '([^']+)'");

        assertThat(stack).contains("'Spring Boot " + springBoot + "'");
        assertThat(stack).contains("'Java " + java + "'");
        assertThat(stack).contains("'HAPI FHIR " + hapi + "'");
    }

    @Test
    void showsOnlyPortsTheComposeFilePublishes() {
        var published = new ArrayList<String>();
        for (var service : services().values()) {
            if (service.get("ports") instanceof List<?> ports) {
                ports.forEach(port -> published.add(port.toString().split(":")[0]));
            }
        }

        var shown = Pattern.compile("port: (\\d+)").matcher(stack).results()
                .map(match -> match.group(1))
                .toList();

        assertThat(shown).isNotEmpty();
        assertThat(published).containsAll(shown);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Map<String, Object>> services() {
        Map<String, Object> compose = new Yaml().load(read("docker-compose.yml"));
        return (Map<String, Map<String, Object>>) compose.get("services");
    }

    private static String find(String text, String regex) {
        var matcher = Pattern.compile(regex).matcher(text);
        assertThat(matcher.find()).as("no match for %s", regex).isTrue();
        return matcher.group(1);
    }

    private static String read(String path) {
        try {
            return Files.readString(ROOT.resolve(path));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
