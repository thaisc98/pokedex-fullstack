package com.pokedex.infrastructure.persistence;

import com.pokedex.domain.identity.DisplayName;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.PasswordHash;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import com.pokedex.infrastructure.persistence.entity.UserEntity;

/** Hand-written {@link User} ↔ {@link UserEntity} mapping. */
final class UserEntityMapper {

    private UserEntityMapper() {}

    static UserEntity toEntity(User user) {
        return new UserEntity(
                user.id().value(),
                user.email().value(),
                user.passwordHash().value(),
                user.displayName().value(),
                user.role().name(),
                user.createdAt(),
                null);
    }

    static User toDomain(UserEntity entity) {
        return new User(
                new UserId(entity.getId()),
                new Email(entity.getEmail()),
                new DisplayName(entity.getDisplayName()),
                new PasswordHash(entity.getPasswordHash()),
                Role.valueOf(entity.getRole()),
                entity.getCreatedAt());
    }
}
