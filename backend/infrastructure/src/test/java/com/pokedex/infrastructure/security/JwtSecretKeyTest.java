package com.pokedex.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * security.md §4.2: the HS256 key is the raw UTF-8 bytes of {@code JWT_SECRET}; the app refuses to start when the
 * secret is missing or shorter than 256 bits (32 bytes). The error never contains the secret.
 */
class JwtSecretKeyTest {

    private static final String SECRET_32 = "0123456789abcdef0123456789abcdef";

    @Test
    void should_use_the_raw_utf8_bytes_as_an_hmac_sha256_key() {
        SecretKey key = JwtSecretKey.fromSecret(SECRET_32);

        assertThat(key.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThat(key.getEncoded()).isEqualTo(SECRET_32.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void should_count_bytes_not_characters() {
        String sixteenTwoByteChars = "é".repeat(16); // 32 bytes

        assertThat(JwtSecretKey.fromSecret(sixteenTwoByteChars).getEncoded()).hasSize(32);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void should_refuse_a_missing_secret(String secret) {
        assertThatThrownBy(() -> JwtSecretKey.fromSecret(secret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void should_refuse_a_secret_shorter_than_256_bits_without_echoing_it() {
        String shortSecret = "0123456789abcdef0123456789abcde"; // 31 bytes

        assertThatThrownBy(() -> JwtSecretKey.fromSecret(shortSecret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits")
                .message()
                .doesNotContain(shortSecret);
    }
}
