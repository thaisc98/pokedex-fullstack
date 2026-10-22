package com.pokedex.application.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.application.identity.port.in.RegisterUserCommand;
import com.pokedex.application.identity.port.in.RegisterUserUseCase;
import com.pokedex.application.identity.service.RegisterUserService;
import com.pokedex.domain.identity.DisplayName;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.shared.EmailAlreadyRegisteredException;
import com.pokedex.domain.shared.InvalidValueException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/**
 * UC-06 {@code RegisterUser}: email, password, displayName → {@code User}; duplicate → 409; weak → 400
 * (architecture.md §1.4, BR-09, security.md §4.1).
 *
 * <p>Design choices: every input is validated <b>before</b> the uniqueness check (400 wins over 409), and the
 * password is hashed only after both, so junk or duplicate requests never pay for BCrypt. A registration is
 * always {@code USER}: the command has no role (mass assignment, security.md §4.3). The race between the
 * existence check and the insert is closed by the repository, whose {@code save} throws
 * {@link EmailAlreadyRegisteredException} on the unique index.
 */
class RegisterUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final String PASSWORD = "Pikachu-Thunder-Meadow-42";

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final InMemoryCommonPasswordList common = new InMemoryCommonPasswordList("password1234");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final RegisterUserUseCase useCase = new RegisterUserService(users, hasher, common, clock);

    @Test
    void should_register_a_user_with_normalized_email_trimmed_name_and_user_role() {
        User user = useCase.register(new RegisterUserCommand("  Ash@Example.COM ", PASSWORD, "  Ash Ketchum "));

        assertThat(user.id()).isNotNull();
        assertThat(user.email()).isEqualTo(new Email("ash@example.com"));
        assertThat(user.displayName()).isEqualTo(new DisplayName("Ash Ketchum"));
        assertThat(user.role()).isEqualTo(Role.USER);
        assertThat(user.createdAt()).isEqualTo(NOW);
    }

    @Test
    void should_save_the_user() {
        User user = useCase.register(command("ash@example.com"));

        assertThat(users.all()).containsExactly(user);
    }

    @Test
    void should_store_only_the_hash_of_the_password() {
        User user = useCase.register(command("ash@example.com"));

        assertThat(hasher.hashed()).containsExactly(PASSWORD);
        assertThat(user.passwordHash()).isEqualTo(FakePasswordHasher.hashOf(PASSWORD));
    }

    @Test
    void should_give_each_registration_its_own_id() {
        User ash = useCase.register(command("ash@example.com"));
        User misty = useCase.register(command("misty@example.com"));

        assertThat(ash.id()).isNotEqualTo(misty.id());
    }

    @Test
    void should_reject_a_taken_email_ignoring_case_and_spaces_without_hashing() {
        useCase.register(command("brock@example.com"));

        assertThatThrownBy(() -> useCase.register(command("  BROCK@example.com ")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        assertThat(users.all()).hasSize(1);
        assertThat(hasher.hashed()).hasSize(1);
    }

    @Test
    void should_report_a_taken_email_when_a_concurrent_registration_wins_the_race() {
        useCase.register(command("brock@example.com"));
        users.racing();

        assertThatThrownBy(() -> useCase.register(command("brock@example.com")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        assertThat(users.all()).hasSize(1);
    }

    @Test
    void should_reject_a_password_that_is_too_short() {
        assertRejected(new RegisterUserCommand("ash@example.com", "elevenchars", "Ash"), "password");
    }

    @Test
    void should_reject_a_password_that_is_too_long() {
        assertRejected(new RegisterUserCommand("ash@example.com", "a".repeat(65), "Ash"), "password");
    }

    @Test
    void should_reject_a_password_on_the_common_list_in_any_case() {
        assertRejected(new RegisterUserCommand("ash@example.com", "password1234", "Ash"), "password");
        assertRejected(new RegisterUserCommand("ash@example.com", "PASSWORD1234", "Ash"), "password");
    }

    @Test
    void should_accept_passwords_of_exactly_12_and_64_characters() {
        useCase.register(new RegisterUserCommand("min@example.com", "Zx9-Qw7-Lm5-", "Twelve"));
        useCase.register(new RegisterUserCommand("max@example.com", "Zx9-Qw7-Lm5-".repeat(5) + "Abcd", "Max"));

        assertThat(users.all()).hasSize(2);
    }

    @Test
    void should_reject_an_invalid_email() {
        assertRejected(new RegisterUserCommand("not-an-email", PASSWORD, "Ash"), "email");
    }

    @Test
    void should_reject_an_invalid_display_name() {
        assertRejected(new RegisterUserCommand("ash@example.com", PASSWORD, "   "), "displayName");
        assertRejected(new RegisterUserCommand("ash@example.com", PASSWORD, "a".repeat(61)), "displayName");
    }

    @Test
    void should_report_invalid_input_before_a_taken_email() {
        useCase.register(command("brock@example.com"));

        assertThatThrownBy(() -> useCase.register(new RegisterUserCommand("brock@example.com", "short", "Brock")))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void should_never_print_the_password_in_the_command() {
        RegisterUserCommand command = new RegisterUserCommand("ash@example.com", PASSWORD, "Ash");

        assertThat(command.toString()).doesNotContain(PASSWORD);
        assertThat(command.toString())
                .isEqualTo("RegisterUserCommand[email=ash@example.com, password=***, displayName=Ash]");
    }

    @Test
    void should_reject_a_null_command() {
        assertThatThrownBy(() -> useCase.register(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_reject_null_collaborators() {
        assertThatThrownBy(() -> new RegisterUserService(null, hasher, common, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RegisterUserService(users, null, common, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RegisterUserService(users, hasher, null, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RegisterUserService(users, hasher, common, null))
                .isInstanceOf(NullPointerException.class);
    }

    private static RegisterUserCommand command(String email) {
        return new RegisterUserCommand(email, PASSWORD, "Trainer");
    }

    private void assertRejected(RegisterUserCommand command, String field) {
        int savesBefore = users.saves().size();
        int hashesBefore = hasher.hashed().size();

        assertThatThrownBy(() -> useCase.register(command))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo(field));

        assertThat(users.saves()).as("nothing saved").hasSize(savesBefore);
        assertThat(hasher.hashed()).as("nothing hashed").hasSize(hashesBefore);
    }
}
