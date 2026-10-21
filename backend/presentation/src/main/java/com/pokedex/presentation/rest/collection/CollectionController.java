package com.pokedex.presentation.rest.collection;

import com.pokedex.application.collection.command.CollectionQuery;
import com.pokedex.application.collection.command.UpdateCustomAttributesCommand;
import com.pokedex.application.collection.port.in.DeleteCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.GetCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.ListCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.SyncPokemonUseCase;
import com.pokedex.application.collection.port.in.SyncResult;
import com.pokedex.application.collection.port.in.UpdateCollectedPokemonUseCase;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.presentation.rest.collection.dto.CollectedPokemonDto;
import com.pokedex.presentation.rest.collection.dto.CustomAttributesDto;
import com.pokedex.presentation.rest.collection.dto.SyncRequest;
import com.pokedex.presentation.rest.collection.dto.UpdateCollectedPokemonRequest;
import com.pokedex.presentation.rest.shared.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The shared team collection (US03, US04, UC-05). Every route needs an authenticated user; DELETE needs ADMIN in the
 * filter chain and again in the use case, which gets the caller's role from here (defence in depth, security.md §3).
 * The OpenAPI annotations mirror {@code docs/api/openapi.yaml}, checked by the contract IT.
 */
@RestController
@RequestMapping(CollectionController.PATH)
@Tag(name = "Collection", description = "Local collection of synced Pokémon with custom attributes.")
public class CollectionController {

    static final String PATH = "/api/v1/collection";
    static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String POKEDEX_ID = "pokedexId";

    private final SyncPokemonUseCase sync;
    private final ListCollectedPokemonUseCase list;
    private final GetCollectedPokemonUseCase getOne;
    private final UpdateCollectedPokemonUseCase update;
    private final DeleteCollectedPokemonUseCase delete;

    public CollectionController(
            SyncPokemonUseCase sync,
            ListCollectedPokemonUseCase list,
            GetCollectedPokemonUseCase getOne,
            UpdateCollectedPokemonUseCase update,
            DeleteCollectedPokemonUseCase delete) {
        this.sync = Objects.requireNonNull(sync, "sync");
        this.list = Objects.requireNonNull(list, "list");
        this.getOne = Objects.requireNonNull(getOne, "getOne");
        this.update = Objects.requireNonNull(update, "update");
        this.delete = Objects.requireNonNull(delete, "delete");
    }

    /** UC-03 / BR-05: 201 + {@code Location} for a new record, 200 for a refresh. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "syncCollection", summary = "Sync a Pokémon into the collection (idempotent upsert)")
    @ApiResponse(responseCode = "201", description = "A new collected Pokémon was created.")
    @ApiResponse(responseCode = "200", description = "An existing collected Pokémon was refreshed.")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid body.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Not authenticated.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Missing or invalid CSRF token.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Not upstream.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "413",
            description = "Body over 16 KB.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "415",
            description = "Not application/json.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "503",
            description = "PokeAPI unavailable.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CollectedPokemonDto> sync(@RequestBody SyncRequest request) {
        PokedexId id = pokedexId(request.pokedexId());
        SyncResult result = sync.sync(id);
        CollectedPokemonDto body = CollectionDtoMapper.toDto(result.pokemon());
        return result.created()
                ? ResponseEntity.created(URI.create(PATH + "/" + body.id())).body(body)
                : ResponseEntity.ok(body);
    }

    /** UC-05: one page of the shared collection; optional exact {@code tag} and name search {@code q}. */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "listCollection", summary = "List collected Pokémon (paginated)")
    @ApiResponse(responseCode = "200", description = "A page of collected Pokémon.")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid paging or filter.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Not authenticated.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<CollectedPokemonDto> list(
            @Parameter(description = "Zero-based page index (BR-01).") @RequestParam(name = "page", defaultValue = "0")
                    int page,
            @Parameter(description = "Page size (BR-01). 1–100, default 20.")
                    @RequestParam(name = "size", defaultValue = "" + PageRequest.DEFAULT_SIZE)
                    int size,
            @Parameter(description = "Exact tag filter.") @RequestParam(name = "tag", required = false) @Nullable String tag,
            @Parameter(description = "Case-insensitive name search (1-50).")
                    @RequestParam(name = "q", required = false)
                    @Nullable String q) {
        return CollectionDtoMapper.toResponse(list.list(CollectionQuery.of(new PageRequest(page, size), tag, q)));
    }

    @GetMapping(path = "/{pokedexId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "getCollectedPokemon", summary = "Get a collected Pokémon by Pokédex id")
    @ApiResponse(responseCode = "200", description = "The collected Pokémon.")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid Pokédex id.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Not authenticated.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Not in the collection.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public CollectedPokemonDto get(@PathVariable(POKEDEX_ID) int pokedexId) {
        return CollectionDtoMapper.toDto(getOne.get(pokedexId(pokedexId)));
    }

    /** UC-04: full replace of the custom attributes (BR-06), guarded by {@code version} (BR-08 → 409). */
    @PutMapping(
            path = "/{pokedexId}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "updateCollectedPokemon", summary = "Replace the custom attributes of a collected Pokémon")
    @ApiResponse(responseCode = "200", description = "The updated collected Pokémon.")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid body or id.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Not authenticated.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Missing or invalid CSRF token.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Not in the collection.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Stale version.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "413",
            description = "Body over 16 KB.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "415",
            description = "Not application/json.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public CollectedPokemonDto update(
            @PathVariable(POKEDEX_ID) int pokedexId, @RequestBody UpdateCollectedPokemonRequest request) {
        PokedexId id = pokedexId(pokedexId);
        CustomAttributesDto custom = request.customAttributes();
        if (custom == null) {
            throw new InvalidValueException(CollectionDtoMapper.CUSTOM, "must not be null");
        }
        Long version = request.version();
        if (version == null) {
            throw new InvalidValueException("version", "must not be null");
        }
        UpdateCustomAttributesCommand command =
                new UpdateCustomAttributesCommand(id, CollectionDtoMapper.toCustomAttributes(custom), version);
        return CollectionDtoMapper.toDto(update.update(command));
    }

    @DeleteMapping("/{pokedexId}")
    @Operation(operationId = "deleteCollectedPokemon", summary = "Delete a collected Pokémon (ADMIN only)")
    @ApiResponse(responseCode = "204", description = "Deleted.")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid Pokédex id.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Not authenticated.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "CSRF token invalid, or not ADMIN.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "404",
            description = "Not in the collection.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> delete(
            @PathVariable(POKEDEX_ID) int pokedexId, @Parameter(hidden = true) Authentication authentication) {
        delete.delete(pokedexId(pokedexId), roleOf(authentication));
        return ResponseEntity.noContent().build();
    }

    /** The caller's highest role: ADMIN if the token carries {@code ROLE_ADMIN}, otherwise USER. */
    static Role roleOf(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (ADMIN_AUTHORITY.equals(authority.getAuthority())) {
                return Role.ADMIN;
            }
        }
        return Role.USER;
    }

    private static PokedexId pokedexId(@Nullable Integer value) {
        if (value == null) {
            throw new InvalidValueException(POKEDEX_ID, "must not be null");
        }
        return new PokedexId(value);
    }
}
