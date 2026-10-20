package com.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** BR-01: page ≥ 0, 1 ≤ size ≤ 100, default size 20 (architecture.md §1.2, §1.3). */
class PageRequestTest {

    @Test
    void should_expose_default_and_maximum_page_size() {
        assertThat(PageRequest.DEFAULT_SIZE).isEqualTo(20);
        assertThat(PageRequest.MAX_SIZE).isEqualTo(100);
    }

    @Test
    void should_use_default_size_of_twenty_when_only_the_page_is_given() {
        PageRequest request = PageRequest.ofPage(3);

        assertThat(request.page()).isEqualTo(3);
        assertThat(request.size()).isEqualTo(20);
    }

    @Test
    void should_accept_the_first_page() {
        assertThat(new PageRequest(0, 20).page()).isZero();
    }

    @Test
    void should_accept_the_largest_page_index() {
        assertThat(new PageRequest(Integer.MAX_VALUE, 20).page()).isEqualTo(Integer.MAX_VALUE);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    void should_reject_a_negative_page(int page) {
        assertThatThrownBy(() -> new PageRequest(page, 20)).isInstanceOfSatisfying(InvalidValueException.class, e -> {
            assertThat(e.field()).isEqualTo("page");
            assertThat(e.getMessage()).isEqualTo("must be greater than or equal to 0");
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 20, 99, 100})
    void should_accept_a_size_between_one_and_one_hundred(int size) {
        assertThat(new PageRequest(0, size).size()).isEqualTo(size);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101, Integer.MAX_VALUE, Integer.MIN_VALUE})
    void should_reject_a_size_outside_one_to_one_hundred(int size) {
        assertThatThrownBy(() -> new PageRequest(0, size)).isInstanceOfSatisfying(InvalidValueException.class, e -> {
            assertThat(e.field()).isEqualTo("size");
            assertThat(e.getMessage()).isEqualTo("must be between 1 and 100");
        });
    }

    @Test
    void should_compute_the_offset_from_page_and_size() {
        assertThat(new PageRequest(0, 20).offset()).isZero();
        assertThat(new PageRequest(1, 20).offset()).isEqualTo(20L);
        assertThat(new PageRequest(3, 7).offset()).isEqualTo(21L);
    }

    @Test
    void should_compute_the_offset_without_int_overflow_for_a_huge_page() {
        assertThat(new PageRequest(Integer.MAX_VALUE, 100).offset()).isEqualTo(Integer.MAX_VALUE * 100L);
    }

    @Test
    void should_be_equal_when_page_and_size_are_equal() {
        assertThat(new PageRequest(2, 20)).isEqualTo(PageRequest.ofPage(2));
    }
}
