package com.pokedex.presentation.rest.catalog;

import com.pokedex.application.catalog.port.in.GetPokemonDetailUseCase;
import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.domain.catalog.vo.PokemonRef;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.presentation.rest.catalog.dto.PokemonDetailDto;
import com.pokedex.presentation.rest.catalog.dto.PokemonSummaryDto;
import com.pokedex.presentation.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only Pokémon catalog (US01, US02). The OpenAPI annotations mirror {@code docs/api/openapi.yaml}
 * ({@code listPokemon}, {@code getPokemonDetail}): exactly the contract's responses, checked by the contract IT.
 */
@RestController
@RequestMapping("/api/v1/pokemon")
@Tag(name = "Catalog", description = "Read-only Pokémon catalog from PokeAPI (US01, US02). Public.")
public class CatalogController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private static final String ID_OR_NAME = "idOrName";
    private static final String ID_OR_NAME_MESSAGE =
            "must be a Pokédex id or a name of 1-50 letters, digits or hyphens";

    private final ListPokemonUseCase listPokemon;
    private final GetPokemonDetailUseCase getPokemonDetail;

    public CatalogController(ListPokemonUseCase listPokemon, GetPokemonDetailUseCase getPokemonDetail) {
        this.listPokemon = listPokemon;
        this.getPokemonDetail = getPokemonDetail;
    }

    /**
     * UC-02. {@code idOrName}: all digits → id ({@code 0} → 404); otherwise lowercased, then {@code
     * ^[a-z0-9-]{1,50}$} (else 400 on field {@code idOrName}). Spring drops {@code ;matrix} content when it
     * binds the path variable, so the raw request URI is checked too: {@code pika;drop} is a 400, never a lookup
     * of {@code pika}.
     */
    @GetMapping("/{idOrName}")
    @Operation(
            operationId = "getPokemonDetail",
            summary = "Get Pokémon detail by numeric id or name",
            description = "US02. All digits → Pokédex id; otherwise lowercased then matched against"
                    + " `^[a-z0-9-]{1,50}$`.")
    @ApiResponse(responseCode = "200", description = "The Pokémon detail.")
    @ApiResponse(
            responseCode = "400",
            description = "Malformed `idOrName` (`/problems/validation-error`).",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "No such Pokémon (`/problems/not-found`).",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "503",
            description = "PokeAPI unavailable, too large or partial (`/problems/upstream-unavailable`).",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public PokemonDetailDto detail(
            @Parameter(description = "Pokédex id (all digits) or name (case-insensitive).")
                    @PathVariable(name = ID_OR_NAME)
                    String idOrName,
            HttpServletRequest request) {
        if (request.getRequestURI().indexOf(';') >= 0) {
            throw new InvalidValueException(ID_OR_NAME, ID_OR_NAME_MESSAGE);
        }
        PokemonRef ref;
        try {
            ref = PokemonRef.parse(idOrName);
        } catch (InvalidValueException e) {
            throw new InvalidValueException(ID_OR_NAME, ID_OR_NAME_MESSAGE);
        }
        return CatalogDtoMapper.toDto(getPokemonDetail.get(ref));
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
