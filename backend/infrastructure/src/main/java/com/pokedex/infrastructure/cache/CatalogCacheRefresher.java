package com.pokedex.infrastructure.cache;

import com.pokedex.infrastructure.pokeapi.FreshResourcesListener;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiPokemonDto;
import com.pokedex.infrastructure.pokeapi.dto.PokeApiSpeciesDto;
import java.util.Objects;
import org.springframework.cache.Cache;

/**
 * After a live re-sync (approved decision): writes the fresh {@code /pokemon/{id}} (also under its name key) and
 * {@code /pokemon-species/{id}} into the per-resource caches and clears the page cache. A sync is rare, so a
 * full page clear is simpler than finding the pages that contain the Pokémon. Only memory is touched here.
 */
public final class CatalogCacheRefresher implements FreshResourcesListener {

    private final Cache pokemon;
    private final Cache species;
    private final Cache pages;

    public CatalogCacheRefresher(Cache pokemon, Cache species, Cache pages) {
        this.pokemon = Objects.requireNonNull(pokemon, "pokemon");
        this.species = Objects.requireNonNull(species, "species");
        this.pages = Objects.requireNonNull(pages, "pages");
    }

    @Override
    public void refreshed(PokeApiPokemonDto freshPokemon, PokeApiSpeciesDto freshSpecies) {
        if (freshPokemon.id() != null) {
            pokemon.put(freshPokemon.id(), freshPokemon);
        }
        if (freshPokemon.name() != null) {
            pokemon.put(CachingPokeApiResources.nameKey(freshPokemon.name()), freshPokemon);
        }
        if (freshSpecies.id() != null) {
            species.put(freshSpecies.id(), freshSpecies);
        }
        pages.clear();
    }
}
