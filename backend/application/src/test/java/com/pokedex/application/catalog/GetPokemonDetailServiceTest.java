package com.pokedex.application.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.application.catalog.port.in.GetPokemonDetailUseCase;
import com.pokedex.application.catalog.service.GetPokemonDetailService;
import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.PokemonRef;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import java.io.IOException;
import org.junit.jupiter.api.Test;

/** UC-02 {@code GetPokemonDetail}: id or name → {@code PokemonDetail}; not found → 404 (architecture.md §1.4). */
class GetPokemonDetailServiceTest {

    private final PokemonDetail pikachu = PokemonDetailFixtures.pikachu();
    private final StubPokemonCatalog catalog = StubPokemonCatalog.withDetails(pikachu);
    private final GetPokemonDetailUseCase useCase = new GetPokemonDetailService(catalog);

    @Test
    void should_return_the_detail_by_pokedex_id() {
        assertThat(useCase.get(new PokemonRef.ById(new PokedexId(25)))).isEqualTo(pikachu);
    }

    @Test
    void should_return_the_detail_by_name() {
        assertThat(useCase.get(new PokemonRef.ByName(new PokemonName("pikachu"))))
                .isEqualTo(pikachu);
    }

    @Test
    void should_ask_the_catalog_with_exactly_the_given_reference() {
        PokemonRef ref = PokemonRef.parse("Pikachu");

        useCase.get(ref);

        assertThat(catalog.detailRequests()).containsExactly(new PokemonRef.ByName(new PokemonName("pikachu")));
    }

    @Test
    void should_propagate_not_found_for_an_unknown_pokemon() {
        assertThatThrownBy(() -> useCase.get(new PokemonRef.ById(new PokedexId(99999))))
                .isInstanceOfSatisfying(
                        NotFoundException.class, e -> assertThat(e.resource()).isEqualTo("pokemon"));
    }

    @Test
    void should_propagate_upstream_unavailable() {
        UpstreamUnavailableException down = new UpstreamUnavailableException("down", new IOException("reset"));
        catalog.failingWith(down);

        assertThatThrownBy(() -> useCase.get(new PokemonRef.ById(new PokedexId(25))))
                .isSameAs(down);
    }

    @Test
    void should_reject_a_null_reference_without_calling_the_catalog() {
        assertThatThrownBy(() -> useCase.get(null)).isInstanceOf(NullPointerException.class);

        assertThat(catalog.detailRequests()).isEmpty();
    }

    @Test
    void should_reject_a_null_catalog() {
        assertThatThrownBy(() -> new GetPokemonDetailService(null)).isInstanceOf(NullPointerException.class);
    }
}
