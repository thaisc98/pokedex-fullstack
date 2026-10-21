package com.pokedex.application.identity.port.out;

import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import java.util.Optional;

/** Stores user accounts. Emails are unique (stored lowercased by {@link Email}). */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    boolean existsByEmail(Email email);

    /**
     * Inserts or updates the user.
     *
     * @throws com.pokedex.domain.shared.EmailAlreadyRegisteredException when another user already has the email,
     *     including a concurrent registration that won the race after {@link #existsByEmail}
     */
    User save(User user);
}
