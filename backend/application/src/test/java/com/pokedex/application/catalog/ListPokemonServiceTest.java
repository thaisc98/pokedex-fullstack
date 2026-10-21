package com.pokedex.application.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.application.catalog.service.ListPokemonService;
import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** UC-01 {@code ListPokemon}: {@code PageRequest} → {@code Page<PokemonSummary>} (architecture.md §1.4). */
class ListPokemonServiceTest {

    private static final List<PokemonSummary> FIVE_POKEMON = IntStream.rangeClosed(1, 5)
            .mapToObj(ListPokemonServiceTest::summary)
            .toList();

    private final StubPokemonCatalog catalog = new StubPokemonCatalog(FIVE_POKEMON);
    private final ListPokemonUseCase useCase = new ListPokemonService(catalog);

    @Test
    void should_return_the_requested_page_from_the_catalog() {
        Page<PokemonSummary> page = useCase.list(new PageRequest(1, 2));

        assertThat(page.content()).extracting(s -> s.id().value()).containsExactly(3, 4);
        assertThat(page.page()).isEqualTo(1);
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalElements()).isEqualTo(5);
        assertThat(page.totalPages()).isEqualTo(3);
    }

    @Test
    void should_ask_the_catalog_for_exactly_the_requested_page() {
        useCase.list(new PageRequest(4, 7));

        assertThat(catalog.requests()).containsExactly(new PageRequest(4, 7));
    }

    @Test
    void should_return_a_short_last_page() {
        Page<PokemonSummary> page = useCase.list(new PageRequest(2, 2));

        assertThat(page.content()).extracting(s -> s.id().value()).containsExactly(5);
    }

    @Test
    void should_return_empty_content_when_the_page_is_past_the_end() {
        Page<PokemonSummary> page = useCase.list(new PageRequest(100_000, 20));

        assertThat(page.content()).isEmpty();
        assertThat(page.page()).isEqualTo(100_000);
        assertThat(page.totalElements()).isEqualTo(5);
    }

    @Test
    void should_propagate_upstream_unavailable_without_a_partial_page() {
        UpstreamUnavailableException down = new UpstreamUnavailableException("down", new IOException("reset"));
        catalog.failingWith(down);

        assertThatThrownBy(() -> useCase.list(PageRequest.ofPage(0))).isSameAs(down);
    }

    @Test
    void should_reject_a_null_request_without_calling_the_catalog() {
        assertThatThrownBy(() -> useCase.list(null)).isInstanceOf(NullPointerException.class);

        assertThat(catalog.requests()).isEmpty();
    }

    @Test
    void should_reject_a_null_catalog() {
        assertThatThrownBy(() -> new ListPokemonService(null)).isInstanceOf(NullPointerException.class);
    }

    private static PokemonSummary summary(int id) {
        return new PokemonSummary(
                new PokedexId(id),
                new PokemonName("pokemon-" + id),
                ImageUrl.PLACEHOLDER,
                Category.UNKNOWN,
                new Mass(id * 10),
                List.of(new Ability("static", false)));
    }
}
