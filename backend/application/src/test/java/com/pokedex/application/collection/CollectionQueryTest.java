package com.pokedex.application.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.application.collection.command.CollectionQuery;
import com.pokedex.domain.collection.vo.Tag;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** UC-05 list filters: page (BR-01), optional exact {@code tag}, optional {@code q} name search (1-50). */
class CollectionQueryTest {

    @Test
    void should_default_to_the_first_page_without_filters() {
        CollectionQuery query = CollectionQuery.firstPage();

        assertThat(query.page()).isEqualTo(PageRequest.ofPage(0));
        assertThat(query.tag()).isNull();
        assertThat(query.name()).isNull();
    }

    @Test
    void should_build_from_raw_parameters() {
        CollectionQuery query = CollectionQuery.of(new PageRequest(1, 5), "starter", "Pika");

        assertThat(query.tag()).isEqualTo(new Tag("starter"));
        assertThat(query.name()).isEqualTo("pika");
    }

    @Test
    void should_treat_absent_filters_as_no_filter() {
        CollectionQuery query = CollectionQuery.of(PageRequest.ofPage(0), null, null);

        assertThat(query.tag()).isNull();
        assertThat(query.name()).isNull();
    }

    @Test
    void should_strip_the_name_search() {
        assertThat(CollectionQuery.of(PageRequest.ofPage(0), null, "  pika ").name())
                .isEqualTo("pika");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void should_reject_a_present_but_blank_name_search(String q) {
        assertThatThrownBy(() -> CollectionQuery.of(PageRequest.ofPage(0), null, q))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("q"));
    }

    @Test
    void should_accept_a_name_search_of_50_characters() {
        assertThat(CollectionQuery.of(PageRequest.ofPage(0), null, "a".repeat(50))
                        .name())
                .hasSize(50);
    }

    @Test
    void should_reject_a_name_search_longer_than_50_characters() {
        assertThatThrownBy(() -> CollectionQuery.of(PageRequest.ofPage(0), null, "a".repeat(51)))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("q"));
    }

    @Test
    void should_keep_like_wildcards_as_literal_text_for_the_adapter_to_escape() {
        assertThat(CollectionQuery.of(PageRequest.ofPage(0), null, "mon_%\\").name())
                .isEqualTo("mon_%\\");
    }

    @Test
    void should_reject_an_invalid_tag_filter() {
        assertThatThrownBy(() -> CollectionQuery.of(PageRequest.ofPage(0), "Fire Type!", null))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("tag"));
    }

    @Test
    void should_reject_a_null_page() {
        assertThatThrownBy(() -> CollectionQuery.of(null, null, null)).isInstanceOf(NullPointerException.class);
    }
}
