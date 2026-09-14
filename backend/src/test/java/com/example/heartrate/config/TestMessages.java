package com.example.heartrate.config;

import org.springframework.context.support.ResourceBundleMessageSource;

/** A {@link Messages} backed by the real properties files, so tests use the real text. */
public final class TestMessages {

    private TestMessages() {}

    public static Messages create() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return new Messages(source);
    }
}
