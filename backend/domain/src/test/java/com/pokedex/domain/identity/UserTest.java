package com.pokedex.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * {@code User} aggregate root (architecture.md §1.2): id, email, display name, password hash, role, plus the
 * {@code created_at} of {@code APP_USER} (§2.7). Holds only the hash, never a password (BR-09). A new
 * registration is always {@code USER}; {@code ADMIN} only exists through rehydration (the seeded admin).
 */
class UserTest {

    private static final UserId ID = new UserId(UUID.fromString("0b6f8f2e-6a43-4b8e-9d0a-6f1d2b3c4d5e"));
    private static final Email EMAIL = new Email("ash@example.com");
    private static final DisplayName NAME = new DisplayName("Ash Ketchum");
    private static final String HASH_VALUE = "{bcrypt}$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW";
    private static final PasswordHash HASH = new PasswordHash(HASH_VALUE);
    private static final Instant CREATED_AT = Instant.parse("2026-10-21T10:00:00Z");

    @Test
    void should_register_a_new_user_with_the_user_role() {
        User user = User.register(ID, EMAIL, NAME, HASH, CREATED_AT);

        assertThat(user.id()).isEqualTo(ID);
        assertThat(user.email()).isEqualTo(EMAIL);
        assertThat(user.displayName()).isEqualTo(NAME);
        assertThat(user.passwordHash()).isEqualTo(HASH);
        assertThat(user.role()).isEqualTo(Role.USER);
        assertThat(user.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    void should_rehydrate_an_admin() {
        User admin = new User(ID, EMAIL, NAME, HASH, Role.ADMIN, CREATED_AT);

        assertThat(admin.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void should_never_print_the_password_hash() {
        User user = User.register(ID, EMAIL, NAME, HASH, CREATED_AT);

        assertThat(user.toString()).doesNotContain(HASH_VALUE).doesNotContain("$2a$");
    }

    @Test
    void should_reject_missing_parts() {
        assertThatThrownBy(() -> new User(null, EMAIL, NAME, HASH, Role.USER, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new User(ID, null, NAME, HASH, Role.USER, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new User(ID, EMAIL, null, HASH, Role.USER, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new User(ID, EMAIL, NAME, null, Role.USER, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new User(ID, EMAIL, NAME, HASH, null, CREATED_AT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new User(ID, EMAIL, NAME, HASH, Role.USER, null))
                .isInstanceOf(NullPointerException.class);
    }
}
