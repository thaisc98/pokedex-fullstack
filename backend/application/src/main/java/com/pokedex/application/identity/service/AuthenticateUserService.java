package com.pokedex.application.identity.service;

import com.pokedex.application.identity.port.in.AuthenticateUserCommand;
import com.pokedex.application.identity.port.in.AuthenticateUserUseCase;
import com.pokedex.application.identity.port.in.AuthenticationResult;
import com.pokedex.application.identity.port.out.PasswordHasher;
import com.pokedex.application.identity.port.out.TokenIssuer;
import com.pokedex.application.identity.port.out.UserRepository;
import com.pokedex.domain.identity.AccessToken;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.shared.AuthenticationFailedException;
import com.pokedex.domain.shared.InvalidValueException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * UC-07. Every failure (unknown email, malformed email, wrong password) runs exactly one hash comparison and throws
 * the same {@link AuthenticationFailedException} (security.md §4.1). The registration policy is not applied here.
 */
public final class AuthenticateUserService implements AuthenticateUserUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final TokenIssuer tokens;
    private final Clock clock;
    private final Duration tokenLifetime;

    public AuthenticateUserService(
            UserRepository users, PasswordHasher hasher, TokenIssuer tokens, Clock clock, Duration tokenLifetime) {
        this.users = Objects.requireNonNull(users, "users");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
        this.tokens = Objects.requireNonNull(tokens, "tokens");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.tokenLifetime = Objects.requireNonNull(tokenLifetime, "tokenLifetime");
        if (tokenLifetime.isZero() || tokenLifetime.isNegative()) {
            throw new IllegalArgumentException("tokenLifetime must be positive");
        }
    }

    @Override
    public AuthenticationResult authenticate(AuthenticateUserCommand command) {
        Objects.requireNonNull(command, "command");
        Optional<User> found = findUser(command.email());
        if (found.isEmpty()) {
            hasher.matches(command.password(), hasher.dummyHash()); // same cost as a real check; result ignored
            throw new AuthenticationFailedException();
        }
        User user = found.get();
        if (!hasher.matches(command.password(), user.passwordHash())) {
            throw new AuthenticationFailedException();
        }
        Instant issuedAt = clock.instant();
        AccessToken token = tokens.issue(user, issuedAt, issuedAt.plus(tokenLifetime));
        return new AuthenticationResult(user, token);
    }

    private Optional<User> findUser(String rawEmail) {
        Email email;
        try {
            email = new Email(rawEmail);
        } catch (InvalidValueException e) {
            return Optional.empty();
        }
        return users.findByEmail(email);
    }
}
