package com.example.heartrate.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.junit.jupiter.api.Test;

class MessagesTest {

    private final ResourceBundle english = ResourceBundle.getBundle("messages", Locale.ENGLISH);
    private final ResourceBundle german = ResourceBundle.getBundle("messages", Locale.GERMAN);

    /**
     * A key present in one language and missing in the other shows up as the raw key on screen —
     * "explain.composition" where a paragraph should be. Nothing else catches that.
     */
    @Test
    void bothLanguagesCarryTheSameKeys() {
        assertThat(german.keySet()).containsExactlyInAnyOrderElementsOf(english.keySet());
        assertThat(english.keySet()).isNotEmpty();
    }

    @Test
    void nothingIsLeftUntranslated() {
        var identical = english.keySet().stream()
                .filter(key -> english.getString(key).equals(german.getString(key)))
                .filter(key -> !key.startsWith("step.")
                        && !key.startsWith("actor.")
                        && !key.equals("sample.bundle.label")
                        && !key.equals("reject.observation"))
                .toList();

        assertThat(identical)
                .as("keys whose German text is still the English one")
                .isEmpty();
    }

    @Test
    void answersInTheRequestedLanguage() {
        var messages = TestMessages.create();

        Locale.setDefault(Locale.ENGLISH);
        org.springframework.context.i18n.LocaleContextHolder.setLocale(Locale.GERMAN);
        assertThat(messages.get("kind.bundle")).isEqualTo("ein FHIR-R4-Bundle");

        org.springframework.context.i18n.LocaleContextHolder.setLocale(Locale.ENGLISH);
        assertThat(messages.get("kind.bundle")).isEqualTo("a FHIR R4 Bundle");
        org.springframework.context.i18n.LocaleContextHolder.resetLocaleContext();
    }

    /**
     * MessageFormat only runs when a message takes arguments, so `''` escapes an apostrophe there
     * and is two literal characters everywhere else. Both spellings are right somewhere and wrong
     * elsewhere, which is exactly the kind of thing to assert rather than remember.
     */
    @Test
    void escapesApostrophesOnlyWhereMessageFormatRuns() {
        for (var bundle : new ResourceBundle[] {english, german}) {
            for (var key : bundle.keySet()) {
                var text = bundle.getString(key);
                if (!text.contains("'")) {
                    continue;
                }
                boolean takesArguments = text.matches("(?s).*\\{\\d+\\}.*");
                assertThat(text.contains("''"))
                        .as("%s: apostrophes must be doubled only when the message has arguments", key)
                        .isEqualTo(takesArguments && text.contains("'"));
            }
        }
    }

    /** An unknown key answers with itself rather than throwing, so one typo cannot blank a page. */
    @Test
    void doesNotThrowOnAnUnknownKey() {
        assertThat(TestMessages.create().get("no.such.key")).isEqualTo("no.such.key");
        assertThat(english.containsKey("no.such.key")).isFalse();
    }

    @Test
    void bundlesAreActuallyThere() {
        assertThat(ResourceBundle.getBundle("messages", Locale.ENGLISH)).isNotNull();
        try {
            ResourceBundle.getBundle("messages", Locale.GERMAN).getString("kind.bundle");
        } catch (MissingResourceException e) {
            throw new AssertionError("German bundle is missing", e);
        }
    }
}
