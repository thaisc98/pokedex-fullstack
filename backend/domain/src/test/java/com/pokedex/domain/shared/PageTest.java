package com.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * UC-01 result type. Mirrors the {@code PageResponse} envelope {content, page, size, totalElements,
 * totalPages} (architecture.md §2.8) and BR-01 (a page past the end is empty content, not an error).
 */
class PageTest {

    @Test
    void should_expose_content_page_size_and_total_elements() {
        Page<String> page = new Page<>(List.of("bulbasaur", "ivysaur"), new PageRequest(0, 2), 1302L);

        assertThat(page.content()).containsExactly("bulbasaur", "ivysaur");
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(1302L);
        assertThat(page.request()).isEqualTo(new PageRequest(0, 2));
    }

    @ParameterizedTest(name = "{0} elements / size {1} -> {2} pages")
    @CsvSource({
        "0,    20, 0",
        "1,    20, 1",
        "19,   20, 1",
        "20,   20, 1",
        "21,   20, 2",
        "1302, 20, 66",
        "1302, 100, 14",
        "1,    1, 1",
        "100,  1, 100"
    })
    void should_compute_total_pages_by_rounding_up(long totalElements, int size, int expectedTotalPages) {
        Page<String> page = new Page<>(List.of(), new PageRequest(0, size), totalElements);

        assertThat(page.totalPages()).isEqualTo(expectedTotalPages);
    }

    @Test
    void should_allow_a_page_past_the_end_with_empty_content() {
        Page<String> page = new Page<>(List.of(), new PageRequest(5, 20), 21L);

        assertThat(page.content()).isEmpty();
        assertThat(page.page()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void should_allow_an_empty_catalog() {
        Page<String> page = new Page<>(List.of(), PageRequest.ofPage(0), 0L);

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
        assertThat(page.totalPages()).isZero();
    }

    @Test
    void should_accept_content_exactly_as_large_as_the_page_size() {
        Page<String> page = new Page<>(List.of("a", "b"), new PageRequest(0, 2), 2L);

        assertThat(page.content()).hasSize(2);
    }

    @Test
    void should_reject_content_larger_than_the_page_size() {
        assertThatThrownBy(() -> new Page<>(List.of("a", "b", "c"), new PageRequest(0, 2), 3L))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("content"));
    }

    @Test
    void should_reject_negative_total_elements() {
        assertThatThrownBy(() -> new Page<>(List.of(), PageRequest.ofPage(0), -1L))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("totalElements"));
    }

    @Test
    void should_reject_null_content_or_request() {
        assertThatThrownBy(() -> new Page<String>(null, PageRequest.ofPage(0), 0L))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Page<>(List.of(), null, 0L)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_keep_content_order_and_copy_it_defensively() {
        List<String> source = new ArrayList<>(List.of("pichu", "pikachu"));
        Page<String> page = new Page<>(source, PageRequest.ofPage(0), 2L);

        source.add("raichu");

        assertThat(page.content()).containsExactly("pichu", "pikachu");
    }

    @Test
    void should_expose_unmodifiable_content() {
        Page<String> page = new Page<>(List.of("pikachu"), PageRequest.ofPage(0), 1L);

        assertThatThrownBy(() -> page.content().add("raichu")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_map_content_and_keep_paging_metadata() {
        Page<String> page = new Page<>(List.of("pikachu", "mr-mime"), new PageRequest(1, 2), 5L);

        Page<Integer> mapped = page.map(String::length);

        assertThat(mapped.content()).containsExactly(7, 7);
        assertThat(mapped.request()).isEqualTo(new PageRequest(1, 2));
        assertThat(mapped.totalElements()).isEqualTo(5L);
        assertThat(mapped.totalPages()).isEqualTo(3);
    }
}
