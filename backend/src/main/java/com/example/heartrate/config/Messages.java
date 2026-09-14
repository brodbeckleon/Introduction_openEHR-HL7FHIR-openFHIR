package com.example.heartrate.config;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * The teaching text, in the language the browser asked for.
 *
 * <p>Most of what this application explains is generated here rather than in the frontend — the
 * pipeline stages, what each mapping did, why a reading was refused. Leaving that in English while
 * translating the buttons around it would translate the packaging and not the content.
 *
 * <p>The locale comes from {@code Accept-Language}, which Spring resolves per request.
 */
@Component
public class Messages {

    private final MessageSource source;

    public Messages(MessageSource source) {
        this.source = source;
    }

    public String get(String key, Object... arguments) {
        return source.getMessage(key, arguments, key, LocaleContextHolder.getLocale());
    }

    /** The language actually in use, so the frontend can tell what it got. */
    public Locale locale() {
        return LocaleContextHolder.getLocale();
    }
}
