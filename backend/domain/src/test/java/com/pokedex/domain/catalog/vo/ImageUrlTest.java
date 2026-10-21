package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * {@code ImageUrl}: what the type itself guarantees. Per security.md §4.5 and openapi {@code ImageUrl},
 * image URLs must be {@code https} on the allow-listed host {@code raw.githubusercontent.com}. The path
 * is further restricted to {@code /PokeAPI/sprites/} to match the frontend {@code next/image
 * remotePatterns} (security.md §5), so any {@code ImageUrl} the API returns is one the UI can render.
 *
 * <p>The server never fetches these URLs; the type only guarantees the value is safe to hand to the
 * browser. Choosing a placeholder for a missing/disallowed upstream URL is the PokeAPI mapper's job.
 */
class ImageUrlTest {

    private static final String PIKACHU_SPRITE =
            "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png";

    @ParameterizedTest
    @ValueSource(
            strings = {
                PIKACHU_SPRITE,
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/25.png",
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/10001.png",
                "https://raw.githubusercontent.com:443/PokeAPI/sprites/master/sprites/pokemon/25.png"
            })
    void should_accept_an_https_sprite_url_on_the_allow_listed_host(String value) {
        assertThat(new ImageUrl(value).value()).isEqualTo(value);
    }

    @Test
    void should_accept_the_host_case_insensitively() {
        String value = "https://RAW.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png";

        assertThat(new ImageUrl(value).value()).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "http://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png",
                "ftp://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png",
                "javascript:alert(1)",
                "data:image/png;base64,iVBORw0KGgo=",
                "file:///etc/passwd"
            })
    void should_reject_a_scheme_other_than_https(String value) {
        assertInvalid(value);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://evil.example/PokeAPI/sprites/master/sprites/pokemon/25.png",
                "https://raw.githubusercontent.com.evil.example/PokeAPI/sprites/master/sprites/pokemon/25.png",
                "https://evilraw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png",
                "https://raw.githubusercontent.com@evil.example/PokeAPI/sprites/master/sprites/pokemon/25.png",
                "https://user:pass@raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png",
                "https://127.0.0.1/PokeAPI/sprites/master/sprites/pokemon/25.png",
                "https://localhost/PokeAPI/sprites/master/sprites/pokemon/25.png"
            })
    void should_reject_a_host_that_is_not_allow_listed_or_carries_user_info(String value) {
        assertInvalid(value);
    }

    @Test
    void should_reject_a_non_default_port() {
        assertInvalid("https://raw.githubusercontent.com:8443/PokeAPI/sprites/master/sprites/pokemon/25.png");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://raw.githubusercontent.com/someone/else/main/x.png",
                "https://raw.githubusercontent.com/",
                "https://raw.githubusercontent.com",
                "https://raw.githubusercontent.com/PokeAPI/sprites/../../evil/repo/x.png",
                "https://raw.githubusercontent.com/PokeAPI/sprites/%2e%2e/%2e%2e/evil/repo/x.png",
                "https://raw.githubusercontent.com/pokeapi/sprites/master/sprites/pokemon/25.png"
            })
    void should_reject_a_path_outside_the_pokeapi_sprites_repository(String value) {
        assertInvalid(value);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                " ",
                "not a url",
                "//raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png",
                "/PokeAPI/sprites/master/sprites/pokemon/25.png",
                "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25 .png",
                " " + PIKACHU_SPRITE,
                PIKACHU_SPRITE + "\n"
            })
    void should_reject_a_blank_relative_or_malformed_url(String value) {
        assertInvalid(value);
    }

    @Test
    void should_accept_a_url_of_exactly_2048_characters() {
        String value = padTo(2048);

        assertThat(new ImageUrl(value).value()).hasSize(2048);
    }

    @Test
    void should_reject_a_url_longer_than_2048_characters() {
        assertInvalid(padTo(2049));
    }

    @Test
    void should_expose_a_placeholder_that_satisfies_the_rules() {
        assertThat(ImageUrl.PLACEHOLDER.value())
                .isEqualTo("https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png");
        assertThat(new ImageUrl(ImageUrl.PLACEHOLDER.value())).isEqualTo(ImageUrl.PLACEHOLDER);
    }

    @Test
    void should_fall_back_to_the_placeholder_when_the_upstream_url_is_missing_or_not_allowed() {
        assertThat(ImageUrl.orPlaceholder(null)).isSameAs(ImageUrl.PLACEHOLDER);
        assertThat(ImageUrl.orPlaceholder("")).isSameAs(ImageUrl.PLACEHOLDER);
        assertThat(ImageUrl.orPlaceholder("https://evil.example/x.png")).isSameAs(ImageUrl.PLACEHOLDER);
        assertThat(ImageUrl.orPlaceholder("http://raw.githubusercontent.com/PokeAPI/sprites/x.png"))
                .isSameAs(ImageUrl.PLACEHOLDER);
    }

    @Test
    void should_keep_an_allowed_upstream_url_when_falling_back() {
        assertThat(ImageUrl.orPlaceholder(PIKACHU_SPRITE)).isEqualTo(new ImageUrl(PIKACHU_SPRITE));
    }

    private static String padTo(int length) {
        String prefix = "https://raw.githubusercontent.com/PokeAPI/sprites/";
        String suffix = ".png";
        return prefix + "a".repeat(length - prefix.length() - suffix.length()) + suffix;
    }

    @Test
    void should_reject_null() {
        assertThatThrownBy(() -> new ImageUrl(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_not_echo_the_rejected_url_in_the_message() {
        String hostile = "https://evil.example/steal?c=1";

        assertThatThrownBy(() -> new ImageUrl(hostile))
                .isInstanceOf(InvalidValueException.class)
                .message()
                .doesNotContain(hostile)
                .doesNotContain("evil.example");
    }

    private static void assertInvalid(String value) {
        assertThatThrownBy(() -> new ImageUrl(value))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("imageUrl"));
    }
}
