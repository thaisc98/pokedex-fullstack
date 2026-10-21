package com.pokedex.application.identity.port.out;

/** The bundled common-password list (BR-09). Entries are lowercase. */
public interface CommonPasswordList {

    boolean contains(String lowercasePassword);
}
