package com.pokedex.domain.collection.vo;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Locale;
import java.util.Objects;

/**
 * A name in one language. {@code lang} is a well-formed BCP-47 tag (checked with
 * {@link Locale#forLanguageTag}) stored in canonical form; {@code value} is 1-60 characters.
 */
public record LocalizedName(String lang, String value) {

    public static final int MAX_LANG_LENGTH = 35;
    public static final int MAX_VALUE_LENGTH = 60;

    private static final String UNDETERMINED = "und";

    public LocalizedName {
        lang = canonicalLanguage(Objects.requireNonNull(lang, "lang"));
        value = Objects.requireNonNull(value, "value").strip();
        int length = value.codePointCount(0, value.length());
        if (length < 1 || length > MAX_VALUE_LENGTH) {
            throw new InvalidValueException("localizedName.value", "must be 1-60 characters");
        }
    }

    private static String canonicalLanguage(String raw) {
        String candidate = raw.strip();
        if (candidate.isEmpty() || candidate.length() > MAX_LANG_LENGTH) {
            throw invalidLanguage();
        }
        String canonical = Locale.forLanguageTag(candidate).toLanguageTag();
        if (UNDETERMINED.equals(canonical) || !canonical.equalsIgnoreCase(candidate)) {
            throw invalidLanguage();
        }
        return canonical;
    }

    private static InvalidValueException invalidLanguage() {
        return new InvalidValueException("localizedName.lang", "must be a well-formed BCP-47 language tag");
    }
}
