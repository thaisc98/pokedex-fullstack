package com.pokedex.application.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.application.identity.port.in.GetCurrentUserUseCase;
import com.pokedex.application.identity.service.GetCurrentUserService;
import com.pokedex.domain.identity.DisplayName;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import com.pokedex.domain.shared.AuthenticationRequiredException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * UC-08 {@code GetCurrentUser}: principal → {@code User}; 401 (architecture.md §1.4). The principal is the
 * JWT {@code sub} parsed into a {@link UserId} by the presentation layer. The user is always read from the
 * repository, so a deleted account with a still-valid token gets 401 instead of stale data.
 */
class GetCurrentUserServiceTest {

    private static final User ASH = new User(
            UserId.random(),
            new Email("ash@example.com"),
            new DisplayName("Ash Ketchum"),
            FakePasswordHasher.hashOf("Squirtle-Wave-Harbor-77"),
            Role.USER,
            Instant.parse("2026-10-21T10:00:00Z"));

    private final InMemoryUserRepository users = new InMemoryUserRepository().with(ASH);
    private final GetCurrentUserUseCase useCase = new GetCurrentUserService(users);

    @Test
    void should_return_the_user_of_the_principal() {
        assertThat(useCase.currentUser(ASH.id())).isEqualTo(ASH);
    }

    @Test
    void should_require_authentication_when_the_user_no_longer_exists() {
        assertThatThrownBy(() -> useCase.currentUser(UserId.random()))
                .isInstanceOf(AuthenticationRequiredException.class);
    }

    @Test
    void should_reject_a_null_principal() {
        assertThatThrownBy(() -> useCase.currentUser(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_reject_a_null_repository() {
        assertThatThrownBy(() -> new GetCurrentUserService(null)).isInstanceOf(NullPointerException.class);
    }
}
