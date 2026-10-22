package com.pokedex.presentation.rest.catalog;

import com.pokedex.application.catalog.port.in.GetPokemonDetailUseCase;
import com.pokedex.application.catalog.port.in.ListPokemonUseCase;
import com.pokedex.application.catalog.port.in.SearchPokemonNamesUseCase;
import com.pokedex.domain.catalog.vo.NameQuery;
import com.pokedex.domain.catalog.vo.PokemonRef;
import com.pokedex.domain.catalog.vo.SearchLimit;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.presentation.rest.catalog.dto.PokemonDetailDto;
import com.pokedex.presentation.rest.catalog.dto.PokemonSearchResultDto;
import com.pokedex.presentation.rest.catalog.dto.PokemonSummaryDto;
import com.pokedex.presentation.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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
    private final SearchPokemonNamesUseCase searchPokemonNames;
    private final CacheControl httpCache;

    static final Duration DEFAULT_MAX_AGE = Duration.ofSeconds(300);
    static final Duration DEFAULT_STALE_WHILE_REVALIDATE = Duration.ofSeconds(86_400);

    /**
     * Name search (autocomplete). A literal {@code /search} mapping, so Spring matches it before {@code
     * /{idOrName}}: {@code search} is never looked up as a Pokémon. {@code q} is checked by {@link NameQuery}, {@code
     * limit} by {@link SearchLimit}; a violation (or a missing {@code q}) is a 400 on that field.
     */
    @GetMapping("/search")
    @Operation(
            operationId = "searchPokemonNames",
            summary = "Search Pokémon names (autocomplete)",
            description = "Prefix matches first, then other matches, each by Pokédex id; at most `limit`.")
    @ApiResponse(responseCode = "200", description = "Matching names (possibly none).")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid `q` or `limit` (`/problems/validation-error`).",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "503",
            description = "The name index cannot be loaded (`/problems/upstream-unavailable`).",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<PokemonSearchResultDto> search(
            @Parameter(description = "Name fragment; trimmed, lowercased, spaces become hyphens.", required = true)
                    @RequestParam(name = "q", required = false)
                    @Nullable String q,
            @Parameter(description = "Maximum results (1–20, default 10).")
                    @RequestParam(name = "limit", defaultValue = "" + SearchLimit.DEFAULT_VALUE)
                    int limit) {
        if (q == null) {
            throw new InvalidValueException("q", "is required");
        }
        NameQuery query = NameQuery.parse(q);
        return cacheable(CatalogDtoMapper.toSearchResult(searchPokemonNames.search(query, new SearchLimit(limit))));
    }

    /** With the default HTTP cache policy (5 min fresh, 1 day stale-while-revalidate). */
    public CatalogController(
            ListPokemonUseCase listPokemon,
            GetPokemonDetailUseCase getPokemonDetail,
            SearchPokemonNamesUseCase searchPokemonNames) {
        this(listPokemon, getPokemonDetail, searchPokemonNames, DEFAULT_MAX_AGE, DEFAULT_STALE_WHILE_REVALIDATE);
    }

    /**
     * @param maxAge how long a shared or browser cache may serve a catalog 200 without asking again
     * @param staleWhileRevalidate how long after that it may still serve it while revalidating in the background
     */
    @Autowired
    public CatalogController(
            ListPokemonUseCase listPokemon,
            GetPokemonDetailUseCase getPokemonDetail,
            SearchPokemonNamesUseCase searchPokemonNames,
            @Value("${pokedex.catalog.http-cache.max-age:300s}") Duration maxAge,
            @Value("${pokedex.catalog.http-cache.stale-while-revalidate:86400s}") Duration staleWhileRevalidate) {
        this.searchPokemonNames = searchPokemonNames;
        this.listPokemon = listPokemon;
        this.getPokemonDetail = getPokemonDetail;
        // security.md §4.7: public, non-personalized catalog data only. Spring Security leaves a Cache-Control that
        // is already set alone; every other response (errors included) keeps its no-store.
        this.httpCache = CacheControl.maxAge(maxAge).cachePublic().staleWhileRevalidate(staleWhileRevalidate);
    }

    private <T> ResponseEntity<T> cacheable(T body) {
        return ResponseEntity.ok().cacheControl(httpCache).body(body);
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
    public ResponseEntity<PokemonDetailDto> detail(
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
        return cacheable(CatalogDtoMapper.toDto(getPokemonDetail.get(ref)));
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
    public ResponseEntity<PageResponse<PokemonSummaryDto>> list(
            @Parameter(description = "Zero-based page index (BR-01).") @RequestParam(name = "page", defaultValue = "0")
                    int page,
            @Parameter(description = "Page size (BR-01). 1–100, default 20.")
                    @RequestParam(name = "size", defaultValue = "" + PageRequest.DEFAULT_SIZE)
                    int size) {
        return cacheable(CatalogDtoMapper.toResponse(listPokemon.list(new PageRequest(page, size))));
    }
}
