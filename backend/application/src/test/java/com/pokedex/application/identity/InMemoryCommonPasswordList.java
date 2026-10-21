package com.pokedex.application.identity;

import com.pokedex.application.identity.port.out.CommonPasswordList;
import java.util.Set;

/** {@link CommonPasswordList} shipped in application's test-jar. Entries are lowercase, like the bundled list. */
public final class InMemoryCommonPasswordList implements CommonPasswordList {

    private final Set<String> passwords;

    public InMemoryCommonPasswordList(String... lowercasePasswords) {
        this.passwords = Set.of(lowercasePasswords);
    }

    @Override
    public boolean contains(String lowercasePassword) {
        return passwords.contains(lowercasePassword);
    }
}
