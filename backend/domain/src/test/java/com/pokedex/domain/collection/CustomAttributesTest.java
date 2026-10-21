package com.pokedex.domain.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.collection.vo.Coordinates;
import com.pokedex.domain.collection.vo.GeoMetadata;
import com.pokedex.domain.collection.vo.LocalizedName;
import com.pokedex.domain.collection.vo.Tag;
import com.pokedex.domain.shared.InvalidValueException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * {@code CustomAttributes}: the only editable part of a collected Pokémon (BR-06). Tags are normalized,
 * de-duplicated and capped at 10 (BR-07); at most one localized name per language.
 */
class CustomAttributesTest {

    @Test
    void should_be_empty_by_default() {
        assertThat(CustomAttributes.EMPTY.localizedNames()).isEmpty();
        assertThat(CustomAttributes.EMPTY.geo()).isNull();
        assertThat(CustomAttributes.EMPTY.tags()).isEmpty();
    }

    @Test
    void should_keep_names_geo_and_tags() {
        GeoMetadata geo = new GeoMetadata("Kanto", "forest", new Coordinates(1, 2));

        CustomAttributes custom =
                new CustomAttributes(Set.of(new LocalizedName("ja", "ピカチュウ")), geo, Set.of(new Tag("starter")));

        assertThat(custom.localizedNames()).containsExactly(new LocalizedName("ja", "ピカチュウ"));
        assertThat(custom.geo()).isEqualTo(geo);
        assertThat(custom.tags()).containsExactly(new Tag("starter"));
    }

    @Test
    void should_normalize_and_deduplicate_raw_tags_before_counting() {
        CustomAttributes custom =
                CustomAttributes.of(List.of(), null, List.of("  Fire-Type ", "fire-type", "STARTER", "starter"));

        assertThat(custom.tags()).containsExactlyInAnyOrder(new Tag("fire-type"), new Tag("starter"));
    }

    @Test
    void should_accept_exactly_ten_tags() {
        assertThat(CustomAttributes.of(List.of(), null, tags(10)).tags()).hasSize(10);
    }

    @Test
    void should_accept_eleven_raw_tags_that_deduplicate_to_ten() {
        List<String> raw = new java.util.ArrayList<>(tags(10));
        raw.add("TAG-0");

        assertThat(CustomAttributes.of(List.of(), null, raw).tags()).hasSize(10);
    }

    @Test
    void should_reject_more_than_ten_distinct_tags() {
        assertThatThrownBy(() -> CustomAttributes.of(List.of(), null, tags(11)))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("tags"));
    }

    @Test
    void should_reject_two_names_for_the_same_language() {
        List<LocalizedName> names = List.of(new LocalizedName("pt-BR", "Pikachu"), new LocalizedName("pt-br", "Pika"));

        assertThatThrownBy(() -> CustomAttributes.of(names, null, List.of()))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("localizedNames"));
    }

    @Test
    void should_accept_one_name_per_language() {
        List<LocalizedName> names = List.of(new LocalizedName("ja", "ピカチュウ"), new LocalizedName("fr", "Pikachu"));

        assertThat(CustomAttributes.of(names, null, List.of()).localizedNames()).hasSize(2);
    }

    @Test
    void should_copy_collections_defensively_and_expose_them_read_only() {
        Set<Tag> tags = new LinkedHashSet<>(Set.of(new Tag("a")));
        CustomAttributes custom = new CustomAttributes(Set.of(), null, tags);

        tags.add(new Tag("b"));

        assertThat(custom.tags()).containsExactly(new Tag("a"));
        assertThatThrownBy(() -> custom.tags().add(new Tag("c"))).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_reject_null_collections() {
        assertThatThrownBy(() -> new CustomAttributes(null, null, Set.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CustomAttributes(Set.of(), null, null)).isInstanceOf(NullPointerException.class);
    }

    private static List<String> tags(int count) {
        return IntStream.range(0, count).mapToObj(i -> "tag-" + i).toList();
    }
}
