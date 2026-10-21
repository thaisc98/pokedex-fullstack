package com.pokedex.infrastructure.security;

import com.pokedex.application.identity.port.out.CommonPasswordList;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The bundled common-password list (BR-09): the top 10,000 of the NCSC "100k most used passwords" list (from
 * HIBP data, via SecLists), lowercased, one per line. Loaded once at startup; a missing file stops the app.
 */
public final class ClasspathCommonPasswordList implements CommonPasswordList {

    public static final String DEFAULT_RESOURCE = "security/common-passwords.txt";

    private final Set<String> passwords;

    private ClasspathCommonPasswordList(Set<String> passwords) {
        this.passwords = passwords;
    }

    public static ClasspathCommonPasswordList load() {
        return load(DEFAULT_RESOURCE);
    }

    static ClasspathCommonPasswordList load(String resource) {
        InputStream in = ClasspathCommonPasswordList.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("common-password list not found on the classpath: " + resource);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return new ClasspathCommonPasswordList(reader.lines()
                    .filter(line -> !line.isEmpty())
                    .map(line -> line.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean contains(String lowercasePassword) {
        return passwords.contains(lowercasePassword);
    }

    int size() {
        return passwords.size();
    }
}
