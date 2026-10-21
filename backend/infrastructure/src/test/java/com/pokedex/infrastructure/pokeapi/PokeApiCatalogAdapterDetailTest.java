package com.pokedex.infrastructure.pokeapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.PokemonRef;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

/**
 * {@code fetchDetail}: {@code /pokemon/{id|name}} → {@code /pokemon-species/{id from species.url}} →
 * {@code /evolution-chain/{id from species.evolution_chain.url}}. Only a 404 on the Pokémon itself means "no such
 * Pokémon"; anything else failing ends as {@link UpstreamUnavailableException} (no partial detail).
 */
class PokeApiCatalogAdapterDetailTest {

    private final FakePokeApiHttpClient client = new FakePokeApiHttpClient();
    private final PokeApiCatalogAdapter adapter =
            new PokeApiCatalogAdapter(new PokeApiGateway(client), new PokeApiMapper());

    @Test
    void should_fetch_pokemon_species_and_chain_by_id_rebuilt_from_upstream_urls() {
        stubPikachu();

        PokemonDetail detail = adapter.fetchDetail(new PokemonRef.ById(new PokedexId(25)));

        assertThat(detail.name().value()).isEqualTo("pikachu");
        assertThat(client.calls()).containsExactly("pokemon/25", "pokemon-species/25", "evolution-chain/10");
    }

    @Test
    void should_look_a_pokemon_up_by_its_validated_name() {
        stubPikachu();
        client.pokemonNamed("pikachu", () -> PokeApiFixtures.pokemon(25));

        PokemonDetail detail = adapter.fetchDetail(new PokemonRef.ByName(new PokemonName("pikachu")));

        assertThat(detail.id().value()).isEqualTo(25);
        assertThat(client.calls()).first().isEqualTo("pokemon/pikachu");
    }

    @Test
    void should_follow_the_species_id_of_a_form_not_the_pokemon_id() {
        client.pokemon(10264, () -> PokeApiFixtures.pokemon(10264))
                .species(1007, () -> PokeApiFixtures.species(1007))
                .evolutionChain(535, () -> PokeApiFixtures.evolutionChain(535));

        adapter.fetchDetail(new PokemonRef.ById(new PokedexId(10264)));

        assertThat(client.calls()).containsExactly("pokemon/10264", "pokemon-species/1007", "evolution-chain/535");
    }

    @Test
    void should_report_not_found_when_pokeapi_has_no_such_pokemon() {
        assertThatThrownBy(() -> adapter.fetchDetail(new PokemonRef.ByName(new PokemonName("missingno"))))
                .isInstanceOfSatisfying(
                        NotFoundException.class, e -> assertThat(e.resource()).isEqualTo("pokemon"));
        assertThat(client.calls()).containsExactly("pokemon/missingno");
    }

    @Test
    void should_report_upstream_unavailable_when_the_species_is_missing() {
        client.pokemon(25, () -> PokeApiFixtures.pokemon(25));

        assertThatThrownBy(() -> adapter.fetchDetail(new PokemonRef.ById(new PokedexId(25))))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void should_report_upstream_unavailable_when_the_chain_is_missing() {
        client.pokemon(25, () -> PokeApiFixtures.pokemon(25)).species(25, () -> PokeApiFixtures.species(25));

        assertThatThrownBy(() -> adapter.fetchDetail(new PokemonRef.ById(new PokedexId(25))))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void should_report_upstream_unavailable_on_server_errors_io_errors_and_unreadable_bodies() {
        stubPikachu();
        client.pokemon(25, () -> {
            throw HttpServerErrorException.create(
                    HttpStatus.BAD_GATEWAY, "Bad Gateway", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        });
        assertThatThrownBy(() -> adapter.fetchDetail(new PokemonRef.ById(new PokedexId(25))))
                .isInstanceOf(UpstreamUnavailableException.class);

        client.pokemon(25, () -> PokeApiFixtures.pokemon(25)).species(25, () -> {
            throw new ResourceAccessException("reset");
        });
        assertThatThrownBy(() -> adapter.fetchDetail(new PokemonRef.ById(new PokedexId(25))))
                .isInstanceOf(UpstreamUnavailableException.class);

        client.species(25, () -> PokeApiFixtures.species(25)).evolutionChain(10, () -> {
            throw new RestClientException("malformed JSON");
        });
        assertThatThrownBy(() -> adapter.fetchDetail(new PokemonRef.ById(new PokedexId(25))))
                .isInstanceOf(UpstreamUnavailableException.class)
                .hasMessage("PokeAPI is unavailable");
    }

    @Test
    void should_report_upstream_unavailable_when_upstream_data_breaks_a_domain_rule() {
        stubPikachu();
        client.pokemon(25, () -> {
            var tree = PokeApiFixtures.tree("pokemon/25.json");
            tree.put("name", "Pikachu!");
            return PokeApiFixtures.convert(tree, com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto.class);
        });

        assertThatThrownBy(() -> adapter.fetchDetail(new PokemonRef.ById(new PokedexId(25))))
                .isInstanceOf(UpstreamUnavailableException.class);
    }

    private void stubPikachu() {
        client.pokemon(25, () -> PokeApiFixtures.pokemon(25))
                .species(25, () -> PokeApiFixtures.species(25))
                .evolutionChain(10, () -> PokeApiFixtures.evolutionChain(10));
    }
}
