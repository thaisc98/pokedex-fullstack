package com.pokedex.presentation.rest.catalog;

import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.presentation.rest.catalog.dto.PokemonSummaryDto;
import com.pokedex.presentation.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only Pokémon catalog (US01). The OpenAPI annotations mirror {@code docs/api/openapi.yaml}
 * ({@code listPokemon}): exactly the 200, 400 and 503 responses, checked by the contract IT.
 */
@RestController
@RequestMapping("/api/v1/pokemon")
@Tag(name = "Catalog", description = "Read-only Pokémon catalog from PokeAPI (US01, US02). Public.")
public class CatalogController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final ListPokemonUseCase listPokemon;

    public CatalogController(ListPokemonUseCase listPokemon) {
        this.listPokemon = listPokemon;
    }

    /**
     * UC-01. {@code page}/{@code size} are checked by {@link PageRequest} (BR-01); a violation or a non-integer
     * value becomes a 400 {@code /problems/validation-error}.
     */
    @GetMapping
    @Operation(
            operationId = "listPokemon",
            summary = "List Pokémon (paginated summaries)",
            description = "US01. BR-01: a page past the end returns empty `content`, not an error.")
    @ApiResponse(responseCode = "200", description = "A page of Pokémon summaries.")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid paging (`/problems/validation-error`).",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "503",
            description = "PokeAPI unavailable, too large or partial (`/problems/upstream-unavailable`).",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<PokemonSummaryDto> list(
            @Parameter(description = "Zero-based page index (BR-01).") @RequestParam(name = "page", defaultValue = "0")
                    int page,
            @Parameter(description = "Page size (BR-01). 1–100, default 20.")
                    @RequestParam(name = "size", defaultValue = "" + PageRequest.DEFAULT_SIZE)
                    int size) {
        return CatalogDtoMapper.toResponse(listPokemon.list(new PageRequest(page, size)));
    }
}
