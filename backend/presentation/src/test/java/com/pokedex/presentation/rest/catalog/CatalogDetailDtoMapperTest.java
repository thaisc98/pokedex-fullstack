package com.pokedex.presentation.rest.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokedex.application.catalog.PokemonDetailFixtures;
import com.pokedex.domain.catalog.vo.EvolutionTrigger;
import com.pokedex.presentation.rest.catalog.dto.AbilityDto;
import com.pokedex.presentation.rest.catalog.dto.BaseStatDto;
import com.pokedex.presentation.rest.catalog.dto.EvolutionNodeDto;
import com.pokedex.presentation.rest.catalog.dto.EvolutionTriggerDto;
import com.pokedex.presentation.rest.catalog.dto.PokemonDetailDto;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Domain → {@code PokemonDetailDto} (openapi), including the JSON shape of the trigger {@code oneOf}. */
class CatalogDetailDtoMapperTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Test
    void should_map_every_contract_field() {
        PokemonDetailDto dto = CatalogDtoMapper.toDto(PokemonDetailFixtures.pikachu());

        assertThat(dto.id()).isEqualTo(25);
        assertThat(dto.name()).isEqualTo("pikachu");
        assertThat(dto.artwork()).endsWith("/other/official-artwork/25.png");
        assertThat(dto.stats()).first().isEqualTo(new BaseStatDto("hp", 35));
        assertThat(dto.stats())
                .extracting(BaseStatDto::name)
                .containsExactly("hp", "attack", "defense", "special-attack", "special-defense", "speed");
        assertThat(dto.description()).startsWith("When several of these POKéMON gather");
        assertThat(dto.types()).containsExactly("electric");
        assertThat(dto.height()).isEqualTo(0.4);
        assertThat(dto.mass()).isEqualTo(6.0);
        assertThat(dto.abilities())
                .containsExactly(new AbilityDto("static", false), new AbilityDto("lightning-rod", true));
    }

    @Test
    void should_map_the_evolution_tree_with_no_trigger_on_the_root() {
        EvolutionNodeDto root = CatalogDtoMapper.toDto(PokemonDetailFixtures.pikachu())
                .evolution()
                .root();

        assertThat(root.speciesId()).isEqualTo(172);
        assertThat(root.name()).isEqualTo("pichu");
        assertThat(root.trigger()).isNull();
        EvolutionNodeDto pikachu = root.evolvesTo().getFirst();
        assertThat(pikachu.trigger()).isEqualTo(new EvolutionTriggerDto.LevelUp("level-up", null));
        assertThat(pikachu.evolvesTo().getFirst().trigger())
                .isEqualTo(new EvolutionTriggerDto.UseItem("use-item", "thunder-stone"));
    }

    @Test
    void should_map_each_trigger_kind() {
        assertThat(CatalogDtoMapper.toDto(EvolutionTrigger.LevelUp.atLevel(16)))
                .isEqualTo(new EvolutionTriggerDto.LevelUp("level-up", 16));
        assertThat(CatalogDtoMapper.toDto(new EvolutionTrigger.UseItem("ice-stone")))
                .isEqualTo(new EvolutionTriggerDto.UseItem("use-item", "ice-stone"));
        assertThat(CatalogDtoMapper.toDto(new EvolutionTrigger.Trade()))
                .isEqualTo(new EvolutionTriggerDto.Trade("trade"));
        assertThat(CatalogDtoMapper.toDto(new EvolutionTrigger.Other("shed")))
                .isEqualTo(new EvolutionTriggerDto.Other("other", "shed"));
    }

    @Test
    void should_serialize_triggers_with_kind_and_omit_an_absent_min_level() {
        JsonNode levelUp = JSON.valueToTree(CatalogDtoMapper.toDto(EvolutionTrigger.LevelUp.anyLevel()));
        JsonNode atLevel = JSON.valueToTree(CatalogDtoMapper.toDto(EvolutionTrigger.LevelUp.atLevel(42)));
        JsonNode trade = JSON.valueToTree(CatalogDtoMapper.toDto(new EvolutionTrigger.Trade()));

        assertThat(levelUp.toString()).isEqualTo("{\"kind\":\"level-up\"}");
        assertThat(atLevel.toString()).isEqualTo("{\"kind\":\"level-up\",\"minLevel\":42}");
        assertThat(trade.toString()).isEqualTo("{\"kind\":\"trade\"}");
    }

    @Test
    void should_serialize_the_root_trigger_as_null_and_the_exact_detail_fields() {
        JsonNode json = JSON.valueToTree(CatalogDtoMapper.toDto(PokemonDetailFixtures.pikachu()));

        assertThat(json.propertyNames())
                .containsExactlyInAnyOrder(
                        "id",
                        "name",
                        "artwork",
                        "stats",
                        "description",
                        "evolution",
                        "types",
                        "height",
                        "mass",
                        "abilities");
        assertThat(json.get("evolution").get("root").has("trigger")).isTrue();
        assertThat(json.get("evolution").get("root").get("trigger").isNull()).isTrue();
        assertThat(json.get("evolution").get("root").propertyNames())
                .containsExactlyInAnyOrder("speciesId", "name", "trigger", "evolvesTo");
    }
}
