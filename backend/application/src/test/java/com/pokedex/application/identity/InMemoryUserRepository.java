package com.pokedex.application.identity;

import com.pokedex.application.identity.port.out.UserRepository;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import com.pokedex.domain.shared.EmailAlreadyRegisteredException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link UserRepository} shipped in application's test-jar. Behaves like the unique index on
 * {@code APP_USER.email}: {@link #save} throws {@link EmailAlreadyRegisteredException} when another user already
 * has the email. {@link #racing()} makes {@link #existsByEmail} always answer {@code false}, to simulate a
 * concurrent registration that wins between the check and the insert.
 */
public final class InMemoryUserRepository implements UserRepository {

    private final Map<UserId, User> users = new LinkedHashMap<>();
    private final List<User> saves = new ArrayList<>();
    private boolean racing;

    /** Stores users directly, bypassing the use cases (test setup). */
    public InMemoryUserRepository with(User... existing) {
        for (User user : existing) {
            users.put(user.id(), user);
        }
        return this;
    }

    /** From now on {@link #existsByEmail} returns {@code false}; {@link #save} still enforces uniqueness. */
    public InMemoryUserRepository racing() {
        this.racing = true;
        return this;
    }

    /** Every user passed to {@link #save}, including rejected ones, in call order. */
    public List<User> saves() {
        return List.copyOf(saves);
    }

    public List<User> all() {
        return List.copyOf(users.values());
    }

    @Override
    public Optional<User> findById(UserId id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return users.values().stream().filter(u -> u.email().equals(email)).findFirst();
    }

    @Override
    public boolean existsByEmail(Email email) {
        return !racing && findByEmail(email).isPresent();
    }

    @Override
    public User save(User user) {
        saves.add(user);
        boolean taken =
                users.values().stream().anyMatch(u -> u.email().equals(user.email()) && !u.id().equals(user.id()));
        if (taken) {
            throw new EmailAlreadyRegisteredException();
        }
        users.put(user.id(), user);
        return user;
    }
}
