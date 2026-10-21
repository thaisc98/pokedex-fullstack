package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * What triggers one evolution step (architecture.md §1.2). The chain root has no trigger. Sealed, so mappers
 * switch over it exhaustively.
 */
public sealed interface EvolutionTrigger
        permits EvolutionTrigger.LevelUp, EvolutionTrigger.UseItem, EvolutionTrigger.Trade, EvolutionTrigger.Other {

    String FIELD = "evolutionTrigger";

    /** Level up, optionally from a minimum level (1–100). */
    record LevelUp(OptionalInt minLevel) implements EvolutionTrigger {

        public LevelUp {
            Objects.requireNonNull(minLevel, "minLevel");
            if (minLevel.isPresent() && (minLevel.getAsInt() < 1 || minLevel.getAsInt() > 100)) {
                throw new InvalidValueException(FIELD, "minimum level must be between 1 and 100");
            }
        }

        public static LevelUp anyLevel() {
            return new LevelUp(OptionalInt.empty());
        }

        public static LevelUp atLevel(int level) {
            return new LevelUp(OptionalInt.of(level));
        }
    }

    /** Use an item, by its PokeAPI slug (e.g. {@code thunder-stone}). */
    record UseItem(String item) implements EvolutionTrigger {

        public UseItem {
            requireSlug(item);
        }
    }

    /** Trade. */
    record Trade() implements EvolutionTrigger {}

    /** Any other trigger, with its raw PokeAPI slug (e.g. {@code shed}). */
    record Other(String name) implements EvolutionTrigger {

        public Other {
            requireSlug(name);
        }
    }

    private static void requireSlug(String value) {
        Objects.requireNonNull(value, "value");
        if (!Slug.isValid(value)) {
            throw new InvalidValueException(FIELD, "must be 1-50 lowercase letters, digits or hyphens");
        }
    }
}
