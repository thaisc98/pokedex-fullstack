package com.pokedex.presentation.rest.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * openapi {@code EvolutionTrigger}: a {@code oneOf} discriminated on {@code kind} ({@code level-up}, {@code
 * use-item}, {@code trade}, {@code other}). Each variant lists exactly its contract fields.
 */
@Schema(
        name = "EvolutionTrigger",
        oneOf = {
            EvolutionTriggerDto.LevelUp.class,
            EvolutionTriggerDto.UseItem.class,
            EvolutionTriggerDto.Trade.class,
            EvolutionTriggerDto.Other.class
        },
        discriminatorProperty = "kind",
        discriminatorMapping = {
            @DiscriminatorMapping(value = "level-up", schema = EvolutionTriggerDto.LevelUp.class),
            @DiscriminatorMapping(value = "use-item", schema = EvolutionTriggerDto.UseItem.class),
            @DiscriminatorMapping(value = "trade", schema = EvolutionTriggerDto.Trade.class),
            @DiscriminatorMapping(value = "other", schema = EvolutionTriggerDto.Other.class)
        })
public sealed interface EvolutionTriggerDto {

    String kind();

    /**
     * {@code {kind: level-up, minLevel?}}. {@code minLevel} is an optional integer in the contract (not nullable),
     * so it is left out when absent.
     */
    @Schema(name = "LevelUpTrigger")
    record LevelUp(
            String kind,

            @JsonInclude(JsonInclude.Include.NON_NULL) @Nullable Integer minLevel) implements EvolutionTriggerDto {}

    /** {@code {kind: use-item, item}}. */
    @Schema(name = "UseItemTrigger")
    record UseItem(String kind, String item) implements EvolutionTriggerDto {}

    /** {@code {kind: trade}}. */
    @Schema(name = "TradeTrigger")
    record Trade(String kind) implements EvolutionTriggerDto {}

    /** {@code {kind: other, name}}. */
    @Schema(name = "OtherTrigger")
    record Other(String kind, String name) implements EvolutionTriggerDto {}
}
