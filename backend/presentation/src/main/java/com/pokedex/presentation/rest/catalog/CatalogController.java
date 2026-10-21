package com.pokedex.presentation.rest.catalog;

import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.presentation.rest.catalog.dto.PokemonSummaryDto;
import com.pokedex.presentation.rest.shared.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Public, read-only Pokémon catalog (US01). */
@RestController
@RequestMapping("/api/v1/pokemon")
public class CatalogController {

    private final ListPokemonUseCase listPokemon;

    public CatalogController(ListPokemonUseCase listPokemon) {
        this.listPokemon = listPokemon;
    }

    /**
     * UC-01. {@code page}/{@code size} are checked by {@link PageRequest} (BR-01); a violation or a non-integer
     * value becomes a 400 {@code /problems/validation-error}.
     */
    @GetMapping
    public PageResponse<PokemonSummaryDto> list(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "" + PageRequest.DEFAULT_SIZE) int size) {
        return CatalogDtoMapper.toResponse(listPokemon.list(new PageRequest(page, size)));
    }
}
