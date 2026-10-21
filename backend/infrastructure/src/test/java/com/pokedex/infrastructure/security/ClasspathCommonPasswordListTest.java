package com.pokedex.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** BR-09: the bundled top-10k common-password list (NCSC / HIBP top 10,000), stored lowercase. */
class ClasspathCommonPasswordListTest {

    private final ClasspathCommonPasswordList list = ClasspathCommonPasswordList.load();

    @Test
    void should_contain_the_documented_example_and_classic_weak_passwords() {
        assertThat(list.contains("password1234")).isTrue(); // testing-strategy §5
        assertThat(list.contains("123456")).isTrue();
        assertThat(list.contains("qwerty")).isTrue();
    }

    @Test
    void should_not_contain_a_strong_passphrase() {
        assertThat(list.contains("pikachu-thunder-meadow-42")).isFalse();
    }

    @Test
    void should_load_about_ten_thousand_entries() {
        assertThat(list.size()).isBetween(9_000, 10_000);
    }

    @Test
    void should_fail_fast_when_the_resource_is_missing() {
        assertThatThrownBy(() -> ClasspathCommonPasswordList.load("security/does-not-exist.txt"))
                .isInstanceOf(IllegalStateException.class);
    }
}
