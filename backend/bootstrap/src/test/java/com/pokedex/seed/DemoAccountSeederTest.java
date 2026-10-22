package com.pokedex.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokedex.application.identity.FakePasswordHasher;
import com.pokedex.application.identity.InMemoryCommonPasswordList;
import com.pokedex.application.identity.InMemoryUserRepository;
import com.pokedex.domain.identity.DisplayName;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Demo accounts ({@code demo} profile, README "Demo credentials"): one ADMIN and one USER from the {@code DEMO_*}
 * variables, through the same domain rules as registration. Idempotent; a missing or weak value skips that account
 * with a WARN naming the variable, never the value.
 */
@ExtendWith(OutputCaptureExtension.class)
class DemoAccountSeederTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final String ADMIN_PHRASE = "Demo-Admin-Passphrase-1";
    private static final String USER_PHRASE = "Demo-Trainer-Passphrase-2";

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final Map<String, String> env = new HashMap<>(Map.of(
            "DEMO_ADMIN_EMAIL",
            " Admin@Pokedex.Local ",
            "DEMO_ADMIN_PASSWORD",
            ADMIN_PHRASE,
            "DEMO_USER_EMAIL",
            "trainer@pokedex.local",
            "DEMO_USER_PASSWORD",
            USER_PHRASE));

    private DemoAccountSeeder seeder() {
        return new DemoAccountSeeder(
                users,
                hasher,
                new InMemoryCommonPasswordList("password1234"),
                Clock.fixed(NOW, ZoneOffset.UTC),
                env::get);
    }

    @Test
    void should_create_an_admin_and_a_user_with_hashed_passwords_and_normalized_emails() {
        assertThat(seeder().seed()).isEqualTo(2);

        User admin = users.findByEmail(new Email("admin@pokedex.local")).orElseThrow();
        User user = users.findByEmail(new Email("trainer@pokedex.local")).orElseThrow();
        assertThat(admin.role()).isEqualTo(Role.ADMIN);
        assertThat(user.role()).isEqualTo(Role.USER);
        assertThat(admin.passwordHash()).isEqualTo(FakePasswordHasher.hashOf(ADMIN_PHRASE));
        assertThat(admin.createdAt()).isEqualTo(NOW);
    }

    @Test
    void should_skip_an_existing_email_without_overwriting_its_password() {
        User existing = new User(
                UserId.random(),
                new Email("trainer@pokedex.local"),
                new DisplayName("Already Here"),
                FakePasswordHasher.hashOf("Original-Passphrase-9"),
                Role.USER,
                NOW);
        users.with(existing);

        assertThat(seeder().seed()).isEqualTo(1);
        assertThat(seeder().seed()).as("second start creates nothing").isZero();

        assertThat(users.findByEmail(existing.email())).contains(existing);
    }

    @Test
    void should_skip_an_account_whose_variable_is_missing_and_name_only_the_variable(CapturedOutput output) {
        env.remove("DEMO_USER_PASSWORD");

        assertThat(seeder().seed()).isEqualTo(1);

        assertThat(users.findByEmail(new Email("trainer@pokedex.local"))).isEmpty();
        assertThat(output.getAll()).contains("WARN", "DEMO_USER_PASSWORD").doesNotContain("trainer@pokedex.local");
    }

    @Test
    void should_skip_a_password_that_breaks_the_policy_without_logging_it(CapturedOutput output) {
        env.put("DEMO_ADMIN_PASSWORD", "password1234");

        assertThat(seeder().seed()).isEqualTo(1);

        assertThat(users.findByEmail(new Email("admin@pokedex.local"))).isEmpty();
        assertThat(output.getAll()).contains("DEMO_ADMIN_PASSWORD").doesNotContain("password1234");
    }

    @Test
    void should_never_log_passwords_or_hashes(CapturedOutput output) {
        seeder().seed();

        assertThat(output.getAll())
                .doesNotContain(ADMIN_PHRASE)
                .doesNotContain(USER_PHRASE)
                .doesNotContain("{fake}");
    }
}
