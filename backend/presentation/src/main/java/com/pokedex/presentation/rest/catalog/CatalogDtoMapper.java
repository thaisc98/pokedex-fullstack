package com.pokedex.presentation.rest.catalog;

import com.pokedex.domain.catalog.EvolutionNode;
import com.pokedex.domain.catalog.PokemonDetail;
import com.pokedex.domain.catalog.PokemonNameRef;
import com.pokedex.domain.catalog.PokemonSummary;
import com.pokedex.domain.catalog.vo.EvolutionTrigger;
import com.pokedex.domain.catalog.vo.PokemonType;
import com.pokedex.domain.shared.Page;
import com.pokedex.presentation.rest.catalog.dto.AbilityDto;
import com.pokedex.presentation.rest.catalog.dto.BaseStatDto;
import com.pokedex.presentation.rest.catalog.dto.EvolutionChainDto;
import com.pokedex.presentation.rest.catalog.dto.EvolutionNodeDto;
import com.pokedex.presentation.rest.catalog.dto.EvolutionTriggerDto;
import com.pokedex.presentation.rest.catalog.dto.PokemonDetailDto;
import com.pokedex.presentation.rest.catalog.dto.PokemonNameRefDto;
import com.pokedex.presentation.rest.catalog.dto.PokemonSearchResultDto;
import com.pokedex.presentation.rest.catalog.dto.PokemonSummaryDto;
import com.pokedex.presentation.rest.shared.PageResponse;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Hand-written domain → REST DTO mapping. Domain objects never leave the presentation layer. */
final class CatalogDtoMapper {

    private CatalogDtoMapper() {}

    static PokemonSummaryDto toDto(PokemonSummary summary) {
        return new PokemonSummaryDto(
                summary.id().value(),
                summary.name().value(),
                summary.sprite().value(),
                summary.category().value(),
                summary.mass().kilograms(),
                summary.abilities().stream()
                        .map(ability -> new AbilityDto(ability.name(), ability.hidden()))
                        .toList(),
                summary.types().stream().map(PokemonType::slug).toList());
    }

    static PokemonSearchResultDto toSearchResult(List<PokemonNameRef> results) {
        return new PokemonSearchResultDto(results.stream()
                .map(ref -> new PokemonNameRefDto(ref.id().value(), ref.name().value()))
                .toList());
    }

    static PageResponse<PokemonSummaryDto> toResponse(Page<PokemonSummary> page) {
        return new PageResponse<>(
                page.content().stream().map(CatalogDtoMapper::toDto).toList(),
                page.page(),
                page.size(),
                page.totalElements(),
                page.totalPages());
    }

    static PokemonDetailDto toDto(PokemonDetail detail) {
        return new PokemonDetailDto(
                detail.id().value(),
                detail.name().value(),
                detail.artwork().value(),
                detail.stats().stream()
                        .map(stat -> new BaseStatDto(stat.name().slug(), stat.value()))
                        .toList(),
                detail.description().value(),
                new EvolutionChainDto(toDto(detail.evolution().root())),
                detail.types().stream().map(PokemonType::slug).toList(),
                detail.height().meters(),
                detail.mass().kilograms(),
                detail.abilities().stream()
                        .map(ability -> new AbilityDto(ability.name(), ability.hidden()))
                        .toList());
    }

    static EvolutionNodeDto toDto(EvolutionNode node) {
        EvolutionTrigger trigger = node.trigger();
        return new EvolutionNodeDto(
                node.speciesId().value(),
                node.name().value(),
                trigger == null ? null : toDto(trigger),
                node.evolvesTo().stream().map(CatalogDtoMapper::toDto).toList());
    }

    static EvolutionTriggerDto toDto(EvolutionTrigger trigger) {
        return switch (trigger) {
            case EvolutionTrigger.LevelUp levelUp -> new EvolutionTriggerDto.LevelUp("level-up", minLevel(levelUp));
            case EvolutionTrigger.UseItem useItem -> new EvolutionTriggerDto.UseItem("use-item", useItem.item());
            case EvolutionTrigger.Trade trade -> new EvolutionTriggerDto.Trade("trade");
            case EvolutionTrigger.Other other -> new EvolutionTriggerDto.Other("other", other.name());
        };
    }

    private static @Nullable Integer minLevel(EvolutionTrigger.LevelUp levelUp) {
        return levelUp.minLevel().isPresent() ? levelUp.minLevel().getAsInt() : null;
    }
}
