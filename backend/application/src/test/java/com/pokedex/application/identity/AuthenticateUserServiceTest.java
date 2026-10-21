package com.pokedex.application.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.application.identity.port.in.AuthenticateUserCommand;
import com.pokedex.application.identity.port.in.AuthenticateUserUseCase;
import com.pokedex.application.identity.port.in.AuthenticationResult;
import com.pokedex.application.identity.service.AuthenticateUserService;
import com.pokedex.domain.identity.DisplayName;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import com.pokedex.domain.shared.AuthenticationFailedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * UC-07 {@code AuthenticateUser}: credentials → user + {@code AccessToken}; bad credentials → 401 with one
 * generic failure (architecture.md §1.4, security.md §4.1).
 *
 * <p>Design choices:
 *
 * <ul>
 *   <li>Unknown email, wrong password and a malformed email all throw the same
 *       {@link AuthenticationFailedException}, and all run exactly one hash comparison: unknown or malformed
 *       emails compare against {@code PasswordHasher.dummyHash()}, so timing does not reveal which emails exist.
 *       A "match" against the dummy hash is never trusted.
 *   <li>The registration password policy is <b>not</b> applied at login: a policy change must not lock out
 *       existing users, and the login only compares hashes.
 *   <li>The use case owns time: {@code issuedAt = clock.instant()}, {@code expiresAt = issuedAt + lifetime}
 *       (60 min in production, injected), and passes both to the {@code TokenIssuer} port, so the response's
 *       {@code expiresAt} and the JWT {@code exp} cannot drift apart.
 * </ul>
 */
class AuthenticateUserServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final Duration LIFETIME = Duration.ofMinutes(60);
    private static final String PASSWORD = "Squirtle-Wave-Harbor-77";

    private static final User ASH = new User(
            new UserId(UUID.fromString("0b6f8f2e-6a43-4b8e-9d0a-6f1d2b3c4d5e")),
            new Email("ash@example.com"),
            new DisplayName("Ash Ketchum"),
            FakePasswordHasher.hashOf(PASSWORD),
            Role.USER,
            NOW.minus(Duration.ofDays(3)));

    private final InMemoryUserRepository users = new InMemoryUserRepository().with(ASH);
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final FakeTokenIssuer tokens = new FakeTokenIssuer();
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AuthenticateUserUseCase useCase = new AuthenticateUserService(users, hasher, tokens, clock, LIFETIME);

    @Test
    void should_return_the_user_and_a_token_for_valid_credentials() {
        AuthenticationResult result = useCase.authenticate(new AuthenticateUserCommand("ash@example.com", PASSWORD));

        assertThat(result.user()).isEqualTo(ASH);
        assertThat(result.accessToken().value())
                .isEqualTo("fake-token-for-" + ASH.id().value());
        assertThat(result.accessToken().expiresAt()).isEqualTo(NOW.plus(LIFETIME));
    }

    @Test
    void should_issue_the_token_for_that_user_from_the_clock_with_the_configured_lifetime() {
        useCase.authenticate(new AuthenticateUserCommand("ash@example.com", PASSWORD));

        assertThat(tokens.issued()).containsExactly(new FakeTokenIssuer.Issued(ASH, NOW, NOW.plus(LIFETIME)));
    }

    @Test
    void should_use_the_injected_token_lifetime() {
        AuthenticateUserUseCase shortLived =
                new AuthenticateUserService(users, hasher, tokens, clock, Duration.ofMinutes(5));

        AuthenticationResult result = shortLived.authenticate(new AuthenticateUserCommand("ash@example.com", PASSWORD));

        assertThat(result.accessToken().expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
    }

    @Test
    void should_find_the_user_whatever_the_email_case_and_spaces() {
        AuthenticationResult result = useCase.authenticate(new AuthenticateUserCommand("  ASH@Example.com ", PASSWORD));

        assertThat(result.user()).isEqualTo(ASH);
    }

    @Test
    void should_issue_a_token_for_an_admin_with_the_admin_user() {
        User admin = new User(
                UserId.random(),
                new Email("oak@example.com"),
                new DisplayName("Professor Oak"),
                FakePasswordHasher.hashOf(PASSWORD),
                Role.ADMIN,
                NOW);
        users.with(admin);

        useCase.authenticate(new AuthenticateUserCommand("oak@example.com", PASSWORD));

        assertThat(tokens.issued()).extracting(FakeTokenIssuer.Issued::user).containsExactly(admin);
    }

    @Test
    void should_fail_for_a_wrong_password_without_issuing_a_token() {
        assertThatThrownBy(() -> useCase.authenticate(new AuthenticateUserCommand("ash@example.com", "Wrong-123456")))
                .isInstanceOf(AuthenticationFailedException.class);

        assertThat(hasher.compared()).containsExactly(ASH.passwordHash());
        assertThat(tokens.issued()).isEmpty();
    }

    @Test
    void should_fail_for_an_unknown_email_after_a_dummy_hash_check() {
        assertThatThrownBy(() -> useCase.authenticate(new AuthenticateUserCommand("nobody@example.com", PASSWORD)))
                .isInstanceOf(AuthenticationFailedException.class);

        assertThat(hasher.compared()).containsExactly(FakePasswordHasher.DUMMY);
        assertThat(tokens.issued()).isEmpty();
    }

    @Test
    void should_fail_for_a_malformed_email_like_an_unknown_one() {
        assertThatThrownBy(() -> useCase.authenticate(new AuthenticateUserCommand("not-an-email", PASSWORD)))
                .isInstanceOf(AuthenticationFailedException.class);

        assertThat(hasher.compared()).containsExactly(FakePasswordHasher.DUMMY);
        assertThat(tokens.issued()).isEmpty();
    }

    @Test
    void should_never_trust_a_match_against_the_dummy_hash() {
        hasher.alwaysMatching();

        assertThatThrownBy(() -> useCase.authenticate(new AuthenticateUserCommand("nobody@example.com", PASSWORD)))
                .isInstanceOf(AuthenticationFailedException.class);
        assertThat(tokens.issued()).isEmpty();
    }

    @Test
    void should_fail_identically_for_unknown_email_and_wrong_password() {
        Throwable wrongPassword = catchFailure(new AuthenticateUserCommand("ash@example.com", "Wrong-123456"));
        Throwable unknownEmail = catchFailure(new AuthenticateUserCommand("nobody@example.com", PASSWORD));
        Throwable malformedEmail = catchFailure(new AuthenticateUserCommand("nobody", PASSWORD));

        assertThat(unknownEmail).hasSameClassAs(wrongPassword).hasMessage(wrongPassword.getMessage());
        assertThat(malformedEmail).hasSameClassAs(wrongPassword).hasMessage(wrongPassword.getMessage());
        assertThat(wrongPassword.getMessage()).doesNotContain("ash@example.com").doesNotContain("Wrong-123456");
    }

    @Test
    void should_not_apply_the_registration_policy_at_login() {
        User legacy = new User(
                UserId.random(),
                new Email("legacy@example.com"),
                new DisplayName("Legacy"),
                FakePasswordHasher.hashOf("password1234"),
                Role.USER,
                NOW);
        users.with(legacy);

        AuthenticationResult result =
                useCase.authenticate(new AuthenticateUserCommand("legacy@example.com", "password1234"));

        assertThat(result.user()).isEqualTo(legacy);
    }

    @Test
    void should_never_print_the_password_in_the_command() {
        assertThat(new AuthenticateUserCommand("ash@example.com", PASSWORD).toString())
                .doesNotContain(PASSWORD);
    }

    @Test
    void should_never_print_the_token_in_the_result() {
        AuthenticationResult result = useCase.authenticate(new AuthenticateUserCommand("ash@example.com", PASSWORD));

        assertThat(result.toString()).doesNotContain(result.accessToken().value());
    }

    @Test
    void should_reject_a_null_command() {
        assertThatThrownBy(() -> useCase.authenticate(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_reject_null_collaborators() {
        assertThatThrownBy(() -> new AuthenticateUserService(null, hasher, tokens, clock, LIFETIME))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuthenticateUserService(users, null, tokens, clock, LIFETIME))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuthenticateUserService(users, hasher, null, clock, LIFETIME))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuthenticateUserService(users, hasher, tokens, null, LIFETIME))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new AuthenticateUserService(users, hasher, tokens, clock, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_reject_a_zero_or_negative_token_lifetime() {
        assertThatThrownBy(() -> new AuthenticateUserService(users, hasher, tokens, clock, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AuthenticateUserService(users, hasher, tokens, clock, Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Throwable catchFailure(AuthenticateUserCommand command) {
        try {
            useCase.authenticate(command);
        } catch (AuthenticationFailedException e) {
            return e;
        }
        throw new AssertionError("expected authentication to fail");
    }
}
