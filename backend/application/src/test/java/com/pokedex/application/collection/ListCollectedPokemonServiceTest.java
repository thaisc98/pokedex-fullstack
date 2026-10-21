package com.pokedex.application.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.application.collection.command.CollectionQuery;
import com.pokedex.application.collection.port.in.ListCollectedPokemonUseCase;
import com.pokedex.application.collection.service.ListCollectedPokemonService;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.collection.vo.CollectedPokemonId;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** UC-05 {@code ListCollectedPokemon}: shared team dataset, paged, optional tag and name filters. */
class ListCollectedPokemonServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-21T10:00:00Z"), ZoneOffset.UTC);

    private final InMemoryCollectedPokemonRepository repository = new InMemoryCollectedPokemonRepository();
    private final ListCollectedPokemonUseCase useCase = new ListCollectedPokemonService(repository);

    @BeforeEach
    void seed() {
        store(4, "charmander", "starter", "fire");
        store(25, "pikachu", "starter");
        store(1, "bulbasaur", "starter");
        store(26, "raichu");
    }

    @Test
    void should_page_in_pokedex_id_order() {
        Page<CollectedPokemon> page = useCase.list(CollectionQuery.of(new PageRequest(0, 3), null, null));

        assertThat(page.content()).extracting(p -> p.pokedexId().value()).containsExactly(1, 4, 25);
        assertThat(page.totalElements()).isEqualTo(4);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void should_filter_by_exact_tag() {
        Page<CollectedPokemon> page = useCase.list(CollectionQuery.of(PageRequest.ofPage(0), "fire", null));

        assertThat(page.content()).extracting(p -> p.pokedexId().value()).containsExactly(4);
    }

    @Test
    void should_search_by_name_case_insensitively() {
        Page<CollectedPokemon> page = useCase.list(CollectionQuery.of(PageRequest.ofPage(0), null, "CHU"));

        assertThat(page.content()).extracting(p -> p.pokedexId().value()).containsExactly(25, 26);
    }

    @Test
    void should_combine_tag_and_name_filters() {
        Page<CollectedPokemon> page = useCase.list(CollectionQuery.of(PageRequest.ofPage(0), "starter", "chu"));

        assertThat(page.content()).extracting(p -> p.pokedexId().value()).containsExactly(25);
    }

    @Test
    void should_return_empty_content_past_the_end() {
        Page<CollectedPokemon> page = useCase.list(CollectionQuery.of(new PageRequest(9, 20), null, null));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(4);
    }

    @Test
    void should_treat_like_wildcards_literally() {
        assertThat(useCase.list(CollectionQuery.of(PageRequest.ofPage(0), null, "%"))
                        .content())
                .isEmpty();
        assertThat(useCase.list(CollectionQuery.of(PageRequest.ofPage(0), null, "_"))
                        .content())
                .isEmpty();
    }

    @Test
    void should_reject_nulls() {
        assertThatThrownBy(() -> useCase.list(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListCollectedPokemonService(null)).isInstanceOf(NullPointerException.class);
    }

    private void store(int id, String name, String... tags) {
        CollectedPokemon pokemon =
                CollectedPokemon.collect(CollectedPokemonId.random(), CollectionFixtures.snapshot(id, name), CLOCK);
        pokemon.updateCustom(CustomAttributes.of(List.of(), null, List.of(tags)), 0, CLOCK);
        repository.save(pokemon);
    }
}
