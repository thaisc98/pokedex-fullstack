package com.pokedex.infrastructure.pokeapi;

import com.pokedex.infrastructure.pokeapi.dto.PokeApiEvolutionChainDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiListDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * PokeAPI HTTP interface client. Paths are relative: the base URL comes only from configuration ({@code
 * pokeapi.base-url}, security.md §4.5). Path variables are typed numbers or names that already passed {@code
 * PokemonName} validation; upstream URLs are never used.
 */
@HttpExchange(accept = "application/json")
public interface PokeApiHttpClient {

    @GetExchange("/pokemon")
    PokeApiListDto list(@RequestParam("offset") long offset, @RequestParam("limit") int limit);

    @GetExchange("/pokemon/{id}")
    PokeApiPokemonDto pokemon(@PathVariable("id") int id);

    @GetExchange("/pokemon/{name}")
    PokeApiPokemonDto pokemonByName(@PathVariable("name") String name);

    @GetExchange("/pokemon-species/{id}")
    PokeApiSpeciesDto species(@PathVariable("id") int id);

    @GetExchange("/evolution-chain/{id}")
    PokeApiEvolutionChainDto evolutionChain(@PathVariable("id") int id);
}
