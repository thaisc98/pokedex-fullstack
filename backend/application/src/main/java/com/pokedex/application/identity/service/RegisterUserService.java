package com.pokedex.application.identity.service;

import com.pokedex.application.identity.port.in.RegisterUserCommand;
import com.pokedex.application.identity.port.in.RegisterUserUseCase;
import com.pokedex.application.identity.port.out.CommonPasswordList;
import com.pokedex.application.identity.port.out.PasswordHasher;
import com.pokedex.application.identity.port.out.UserRepository;
import com.pokedex.domain.identity.DisplayName;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.PasswordPolicy;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import com.pokedex.domain.shared.EmailAlreadyRegisteredException;
import java.time.Clock;
import java.util.Objects;

/**
 * UC-06. Validates all input first (400 before 409), then checks the email is free, then hashes: invalid or
 * duplicate requests never pay for BCrypt. Plain Java; turned into a bean in bootstrap.
 */
public final class RegisterUserService implements RegisterUserUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final PasswordPolicy policy;
    private final Clock clock;

    public RegisterUserService(
            UserRepository users, PasswordHasher hasher, CommonPasswordList commonPasswords, Clock clock) {
        this.users = Objects.requireNonNull(users, "users");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
        Objects.requireNonNull(commonPasswords, "commonPasswords");
        this.policy = new PasswordPolicy(commonPasswords::contains);
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public User register(RegisterUserCommand command) {
        Objects.requireNonNull(command, "command");
        Email email = new Email(command.email());
        DisplayName displayName = new DisplayName(command.displayName());
        policy.check(command.password());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        User user =
                User.register(UserId.random(), email, displayName, hasher.hash(command.password()), clock.instant());
        return users.save(user);
    }
}
