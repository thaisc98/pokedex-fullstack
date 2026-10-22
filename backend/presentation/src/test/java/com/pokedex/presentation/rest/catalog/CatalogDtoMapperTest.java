package com.pokedex.presentation.rest.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.catalog.vo.Ability;
import com.pokedex.domain.catalog.vo.Category;
import com.pokedex.domain.catalog.vo.ImageUrl;
import com.pokedex.domain.catalog.vo.Mass;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import com.pokedex.domain.catalog.vo.PokemonType;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.presentation.rest.catalog.dto.AbilityDto;
import com.pokedex.presentation.rest.catalog.dto.PokemonSummaryDto;
import com.pokedex.presentation.rest.shared.PageResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Domain → REST DTO mapping for US01 (openapi {@code PokemonSummaryDto}, {@code PageResponse}). */
class CatalogDtoMapperTest {

    static final String SPRITE = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/25.png";

    static PokemonSummary pikachu() {
        return new PokemonSummary(
                new PokedexId(25),
                new PokemonName("pikachu"),
                new ImageUrl(SPRITE),
                new Category("Mouse Pokémon"),
                new Mass(60),
                List.of(new Ability("static", false), new Ability("lightning-rod", true)),
                List.of(PokemonType.ELECTRIC));
    }

    @Test
    void should_map_a_summary_to_the_contract_fields_with_mass_in_kilograms() {
        PokemonSummaryDto dto = CatalogDtoMapper.toDto(pikachu());

        assertThat(dto)
                .isEqualTo(new PokemonSummaryDto(
                        25,
                        "pikachu",
                        SPRITE,
                        "Mouse Pokémon",
                        6.0,
                        List.of(new AbilityDto("static", false), new AbilityDto("lightning-rod", true)),
                        List.of("electric")));
    }

    @Test
    void should_map_an_unknown_category_to_its_text() {
        PokemonSummary summary = new PokemonSummary(
                new PokedexId(1),
                new PokemonName("x"),
                ImageUrl.PLACEHOLDER,
                Category.UNKNOWN,
                new Mass(0),
                List.of(),
                List.of(PokemonType.FIRE, PokemonType.FLYING));

        PokemonSummaryDto dto = CatalogDtoMapper.toDto(summary);

        assertThat(dto.category()).isEqualTo("Unknown");
        assertThat(dto.sprite()).isEqualTo(ImageUrl.PLACEHOLDER.value());
        assertThat(dto.mass()).isZero();
        assertThat(dto.abilities()).isEmpty();
        assertThat(dto.types()).containsExactly("fire", "flying");
    }

    @Test
    void should_map_the_page_envelope() {
        Page<PokemonSummary> page = new Page<>(List.of(pikachu()), new PageRequest(2, 10), 21);

        PageResponse<PokemonSummaryDto> response = CatalogDtoMapper.toResponse(page);

        assertThat(response.content()).containsExactly(CatalogDtoMapper.toDto(pikachu()));
        assertThat(response.page()).isEqualTo(2);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isEqualTo(21);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void should_map_an_empty_page_past_the_end() {
        PageResponse<PokemonSummaryDto> response =
                CatalogDtoMapper.toResponse(new Page<>(List.of(), new PageRequest(100_000, 20), 1351));

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(100_000);
        assertThat(response.totalPages()).isEqualTo(68);
    }
}
