package com.pokedex.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pokedex.domain.identity.DisplayName;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.PasswordHash;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import com.pokedex.domain.shared.EmailAlreadyRegisteredException;
import com.pokedex.infrastructure.persistence.entity.UserEntity;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * {@code UserRepository} adapter: maps {@link User} ↔ {@link UserEntity} by hand (entities never leave
 * infrastructure) and turns the unique-email violation into {@link EmailAlreadyRegisteredException}. The SQL itself
 * is covered by the PostgreSQL ITs.
 */
class JpaUserRepositoryAdapterTest {

    private static final User ASH = new User(
            UserId.random(),
            new Email("ash@example.com"),
            new DisplayName("Ash Ketchum"),
            new PasswordHash("{bcrypt}$2a$12$abcdefghijklmnopqrstuv"),
            Role.ADMIN,
            Instant.parse("2026-10-21T10:00:00Z"));

    private final SpringDataUserRepository springData = mock(SpringDataUserRepository.class);
    private final JpaUserRepositoryAdapter adapter = new JpaUserRepositoryAdapter(springData);

    @Test
    void should_map_every_field_both_ways() {
        UserEntity entity = UserEntityMapper.toEntity(ASH);

        assertThat(UserEntityMapper.toDomain(entity)).isEqualTo(ASH);
        assertThat(entity.getEmail()).isEqualTo("ash@example.com");
        assertThat(entity.getRole()).isEqualTo("ADMIN");
        assertThat(entity.getVersion()).as("new entities are persisted, not merged").isNull();
    }

    @Test
    void should_find_by_id_and_by_email() {
        UserEntity entity = UserEntityMapper.toEntity(ASH);
        when(springData.findById(ASH.id().value())).thenReturn(Optional.of(entity));
        when(springData.findByEmail("ash@example.com")).thenReturn(Optional.of(entity));

        assertThat(adapter.findById(ASH.id())).contains(ASH);
        assertThat(adapter.findByEmail(ASH.email())).contains(ASH);
        assertThat(adapter.findById(UserId.random())).isEmpty();
    }

    @Test
    void should_check_existence_by_email() {
        when(springData.existsByEmail("ash@example.com")).thenReturn(true);

        assertThat(adapter.existsByEmail(ASH.email())).isTrue();
        assertThat(adapter.existsByEmail(new Email("misty@example.com"))).isFalse();
    }

    @Test
    void should_save_and_return_the_stored_user() {
        when(springData.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        assertThat(adapter.save(ASH)).isEqualTo(ASH);
    }

    @Test
    void should_translate_the_unique_email_violation() {
        when(springData.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate",
                        new ConstraintViolationException(
                                "duplicate key", new SQLException("23505"), JpaUserRepositoryAdapter.EMAIL_UNIQUE)));

        assertThatThrownBy(() -> adapter.save(ASH)).isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void should_rethrow_any_other_integrity_violation() {
        DataIntegrityViolationException other = new DataIntegrityViolationException(
                "check", new ConstraintViolationException("check", new SQLException("23514"), "ck_app_user_role"));
        when(springData.saveAndFlush(any())).thenThrow(other);

        assertThatThrownBy(() -> adapter.save(ASH)).isSameAs(other);
    }
}
