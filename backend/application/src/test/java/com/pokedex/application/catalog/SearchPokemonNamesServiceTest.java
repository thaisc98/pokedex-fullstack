package com.pokedex.application.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.application.catalog.port.in.SearchPokemonNamesUseCase;
import com.pokedex.application.catalog.service.SearchPokemonNamesService;
import com.pokedex.domain.catalog.PokemonNameRef;
import com.pokedex.domain.catalog.vo.NameQuery;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.SearchLimit;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Name search (autocomplete): names starting with the query first, then names containing it; each group by
 * Pokédex id; at most {@code limit}; no match → empty. Filtering is in memory over the catalog's name index.
 */
class SearchPokemonNamesServiceTest {

    // deliberately not in id order, to prove the sorting
    private static final List<PokemonNameRef> INDEX = List.of(
            ref(26, "raichu"),
            ref(10080, "pikachu-rock-star"),
            ref(25, "pikachu"),
            ref(172, "pichu"),
            ref(122, "mr-mime"),
            ref(866, "mr-rime"),
            ref(439, "mime-jr"),
            ref(1, "bulbasaur"));

    private final StubPokemonCatalog catalog = StubPokemonCatalog.withNameIndex(INDEX);
    private final SearchPokemonNamesUseCase useCase = new SearchPokemonNamesService(catalog);

    @Test
    void should_return_prefix_matches_by_id_before_other_matches_by_id() {
        List<PokemonNameRef> results = useCase.search(NameQuery.parse("chu"), SearchLimit.DEFAULT);

        // none starts with "chu"; contained: pikachu (25), raichu (26), pichu (172), pikachu-rock-star (10080)
        assertThat(results).extracting(r -> r.id().value()).containsExactly(25, 26, 172, 10080);
    }

    @Test
    void should_put_names_that_start_with_the_query_first() {
        List<PokemonNameRef> results = useCase.search(NameQuery.parse("mi"), SearchLimit.DEFAULT);

        // starts with "mi": mime-jr (439); contains "mi": mr-mime (122)
        assertThat(results).extracting(r -> r.name().value()).containsExactly("mime-jr", "mr-mime");
    }

    @Test
    void should_order_each_group_by_pokedex_id() {
        List<PokemonNameRef> results = useCase.search(NameQuery.parse("pi"), SearchLimit.DEFAULT);

        assertThat(results).extracting(r -> r.name().value()).containsExactly("pikachu", "pichu", "pikachu-rock-star");
    }

    @Test
    void should_stop_at_the_limit_keeping_prefix_matches_first() {
        List<PokemonNameRef> results = useCase.search(NameQuery.parse("chu"), new SearchLimit(2));

        assertThat(results).extracting(r -> r.id().value()).containsExactly(25, 26);
    }

    @Test
    void should_match_the_normalized_query() {
        List<PokemonNameRef> results = useCase.search(NameQuery.parse("Mr Mime"), SearchLimit.DEFAULT);

        assertThat(results).extracting(r -> r.name().value()).containsExactly("mr-mime");
    }

    @Test
    void should_return_an_empty_list_without_a_match() {
        assertThat(useCase.search(NameQuery.parse("zzz"), SearchLimit.DEFAULT)).isEmpty();
    }

    @Test
    void should_list_each_pokemon_once_even_if_it_matches_both_ways() {
        List<PokemonNameRef> results = useCase.search(NameQuery.parse("pikachu"), SearchLimit.DEFAULT);

        assertThat(results).extracting(r -> r.name().value()).containsExactly("pikachu", "pikachu-rock-star");
    }

    @Test
    void should_read_the_name_index_once_per_search() {
        useCase.search(NameQuery.parse("pi"), SearchLimit.DEFAULT);

        assertThat(catalog.nameIndexRequests()).isEqualTo(1);
    }

    @Test
    void should_propagate_upstream_unavailable() {
        UpstreamUnavailableException down = new UpstreamUnavailableException("down", new IOException("reset"));
        catalog.failingWith(down);

        assertThatThrownBy(() -> useCase.search(NameQuery.parse("pi"), SearchLimit.DEFAULT))
                .isSameAs(down);
    }

    @Test
    void should_reject_null_arguments() {
        assertThatThrownBy(() -> useCase.search(null, SearchLimit.DEFAULT)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> useCase.search(NameQuery.parse("pi"), null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new SearchPokemonNamesService(null)).isInstanceOf(NullPointerException.class);
        assertThat(catalog.nameIndexRequests()).isZero();
    }

    private static PokemonNameRef ref(int id, String name) {
        return new PokemonNameRef(new PokedexId(id), new PokemonName(name));
    }
}
