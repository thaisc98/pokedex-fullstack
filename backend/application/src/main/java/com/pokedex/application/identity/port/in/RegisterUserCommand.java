package com.pokedex.application.identity.port.in;

import java.util.Objects;

/** Raw registration input. {@link #toString()} never shows the password. */
public record RegisterUserCommand(String email, String password, String displayName) {

    public RegisterUserCommand {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(displayName, "displayName");
    }

    @Override
    public String toString() {
        return "RegisterUserCommand[email=" + email + ", password=***, displayName=" + displayName + "]";
    }
}
