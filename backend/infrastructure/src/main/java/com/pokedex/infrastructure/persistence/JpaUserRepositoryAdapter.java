package com.pokedex.infrastructure.persistence;

import com.pokedex.application.identity.port.out.UserRepository;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import com.pokedex.domain.shared.EmailAlreadyRegisteredException;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * {@link UserRepository} on Spring Data JPA. Users are only ever inserted (registration), so {@link #save} flushes
 * immediately and the unique index on {@code email} (constraint {@value #EMAIL_UNIQUE}) closes the race between
 * the existence check and the insert.
 */
public final class JpaUserRepositoryAdapter implements UserRepository {

    static final String EMAIL_UNIQUE = "uk_app_user_email";

    private final SpringDataUserRepository users;

    public JpaUserRepositoryAdapter(SpringDataUserRepository users) {
        this.users = Objects.requireNonNull(users, "users");
    }

    @Override
    public Optional<User> findById(UserId id) {
        return users.findById(id.value()).map(UserEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return users.findByEmail(email.value()).map(UserEntityMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return users.existsByEmail(email.value());
    }

    @Override
    public User save(User user) {
        try {
            return UserEntityMapper.toDomain(users.saveAndFlush(UserEntityMapper.toEntity(user)));
        } catch (DataIntegrityViolationException e) {
            if (EMAIL_UNIQUE.equalsIgnoreCase(constraintName(e))) {
                throw new EmailAlreadyRegisteredException();
            }
            throw e;
        }
    }

    private static @Nullable String constraintName(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }
        return null;
    }
}
