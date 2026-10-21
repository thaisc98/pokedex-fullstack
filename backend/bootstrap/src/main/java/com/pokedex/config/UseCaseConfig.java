package com.pokedex.config;

import com.pokedex.application.catalog.port.in.GetPokemonDetailUseCase;
import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.application.catalog.port.out.PokemonCatalogPort;
import com.pokedex.application.catalog.service.GetPokemonDetailService;
import com.pokedex.application.catalog.service.ListPokemonService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Turns the plain use cases into Spring beans here in bootstrap, so {@code application} needs no
 * {@code @Service}.
 */
@Configuration(proxyBeanMethods = false)
public class UseCaseConfig {

    @Bean
    ListPokemonUseCase listPokemonUseCase(PokemonCatalogPort pokemonCatalog) {
        return new ListPokemonService(pokemonCatalog);
    }

    @Bean
    GetPokemonDetailUseCase getPokemonDetailUseCase(PokemonCatalogPort pokemonCatalog) {
        return new GetPokemonDetailService(pokemonCatalog);
    }
}
