package com.pokedex.seed;

import com.pokedex.application.identity.port.out.CommonPasswordList;
import com.pokedex.application.identity.port.out.PasswordHasher;
import com.pokedex.application.identity.port.out.UserRepository;
import com.pokedex.domain.identity.DisplayName;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.PasswordPolicy;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import com.pokedex.domain.shared.InvalidValueException;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demo profile only: one ADMIN and one USER from the {@code DEMO_*} variables (README "Demo credentials"). Same rules
 * as registration (email normalization, password policy, BCrypt); the ADMIN is created here, by rehydration, never
 * through the public API. Idempotent: an existing email is left untouched. Logs variable names, never values.
 */
public final class DemoAccountSeeder {

    /** One seeded account and the variables it comes from. */
    public record Account(String emailVariable, String passwordVariable, String displayName, Role role) {}

    public static final List<Account> ACCOUNTS = List.of(
            new Account("DEMO_ADMIN_EMAIL", "DEMO_ADMIN_PASSWORD", "Demo Admin", Role.ADMIN),
            new Account("DEMO_USER_EMAIL", "DEMO_USER_PASSWORD", "Demo Trainer", Role.USER));

    private static final Logger LOG = LoggerFactory.getLogger(DemoAccountSeeder.class);

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final PasswordPolicy policy;
    private final Clock clock;
    private final Function<String, @Nullable String> environment;

    public DemoAccountSeeder(
            UserRepository users,
            PasswordHasher hasher,
            CommonPasswordList commonPasswords,
            Clock clock,
            Function<String, @Nullable String> environment) {
        this.users = Objects.requireNonNull(users, "users");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
        this.policy = new PasswordPolicy(Objects.requireNonNull(commonPasswords, "commonPasswords")::contains);
        this.clock = Objects.requireNonNull(clock, "clock");
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    /**
     * True when at least one {@code DEMO_*} variable is set. A demo-profile run without them (e.g. the contract IT)
     * seeds nothing at all, so it never calls PokeAPI or writes demo data.
     */
    public static boolean isConfigured(Function<String, @Nullable String> environment) {
        return ACCOUNTS.stream()
                .flatMap(account -> Stream.of(account.emailVariable(), account.passwordVariable()))
                .map(environment)
                .anyMatch(value -> value != null && !value.isBlank());
    }

    /** @return how many accounts were created */
    public int seed() {
        int created = 0;
        for (Account account : ACCOUNTS) {
            if (seed(account)) {
                created++;
            }
        }
        return created;
    }

    private boolean seed(Account account) {
        String rawEmail = variable(account.emailVariable());
        String password = variable(account.passwordVariable());
        if (rawEmail == null || password == null) {
            return false;
        }
        Email email;
        try {
            email = new Email(rawEmail);
        } catch (InvalidValueException e) {
            LOG.warn(
                    "Demo seed: {} is not a valid email; skipping the {} account",
                    account.emailVariable(),
                    account.role());
            return false;
        }
        if (users.existsByEmail(email)) {
            LOG.info("Demo seed: the {} account already exists; left unchanged", account.role());
            return false;
        }
        try {
            policy.check(password);
        } catch (InvalidValueException e) {
            LOG.warn(
                    "Demo seed: {} does not meet the password policy; skipping the {} account",
                    account.passwordVariable(),
                    account.role());
            return false;
        }
        users.save(new User(
                UserId.random(),
                email,
                new DisplayName(account.displayName()),
                hasher.hash(password),
                account.role(),
                clock.instant()));
        LOG.info("Demo seed: created the {} account", account.role());
        return true;
    }

    private @Nullable String variable(String name) {
        String value = environment.apply(name);
        if (value == null || value.isBlank()) {
            LOG.warn("Demo seed: {} is not set; skipping that account", name);
            return null;
        }
        return value;
    }
}
