package com.pokedex.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokedex.domain.identity.PasswordHash;
import java.security.SecureRandom;
import org.junit.jupiter.api.Test;

/**
 * {@code PasswordHasher} adapter (security.md §4.1): {@code DelegatingPasswordEncoder} with BCrypt strength 12, so
 * hashes are {@code {bcrypt}}-prefixed. Most tests use strength 4 to stay fast; one pins the production strength.
 */
class BCryptPasswordHasherTest {

    private static final String PASSWORD = "Pikachu-Thunder-Meadow-42";

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher(4, new SecureRandom());

    @Test
    void should_use_bcrypt_strength_12_behind_the_bcrypt_prefix_by_default() {
        PasswordHash hash = new BCryptPasswordHasher().hash(PASSWORD);

        assertThat(hash.value()).startsWith("{bcrypt}$2a$12$");
    }

    @Test
    void should_match_the_right_password_only() {
        PasswordHash hash = hasher.hash(PASSWORD);

        assertThat(hasher.matches(PASSWORD, hash)).isTrue();
        assertThat(hasher.matches("Pikachu-Thunder-Meadow-43", hash)).isFalse();
    }

    @Test
    void should_salt_every_hash() {
        assertThat(hasher.hash(PASSWORD)).isNotEqualTo(hasher.hash(PASSWORD));
    }

    @Test
    void should_never_contain_the_password_in_the_hash() {
        assertThat(hasher.hash(PASSWORD).value()).doesNotContain(PASSWORD);
    }

    @Test
    void should_offer_a_dummy_hash_of_the_same_kind_that_matches_no_ordinary_password() {
        PasswordHash dummy = hasher.dummyHash();

        assertThat(dummy.value()).startsWith("{bcrypt}$2a$04$");
        assertThat(hasher.dummyHash()).as("computed once").isSameAs(dummy);
        assertThat(hasher.matches(PASSWORD, dummy)).isFalse();
        assertThat(hasher.matches("", dummy)).isFalse();
    }

    @Test
    void should_answer_false_instead_of_failing_for_an_unknown_encoder_id() {
        assertThat(hasher.matches(PASSWORD, new PasswordHash("{md5}5f4dcc3b5aa765d61d8327deb882cf99")))
                .isFalse();
    }

    @Test
    void should_answer_false_instead_of_failing_for_a_password_over_72_bytes() {
        PasswordHash hash = hasher.hash(PASSWORD);

        assertThat(hasher.matches("a".repeat(100), hash)).isFalse();
    }
}
