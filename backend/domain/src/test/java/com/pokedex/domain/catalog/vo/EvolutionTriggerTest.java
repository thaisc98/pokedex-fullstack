package com.pokedex.domain.catalog.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Sealed {@code EvolutionTrigger}: {@code LevelUp(minLevel?)}, {@code UseItem(item)}, {@code Trade}, {@code
 * Other(name)} (architecture.md §1.2, openapi {@code EvolutionTrigger}).
 */
class EvolutionTriggerTest {

    @Test
    void should_be_sealed_to_exactly_the_four_kinds() {
        assertThat(EvolutionTrigger.class.isSealed()).isTrue();
        assertThat(EvolutionTrigger.class.getPermittedSubclasses())
                .containsExactlyInAnyOrder(
                        EvolutionTrigger.LevelUp.class,
                        EvolutionTrigger.UseItem.class,
                        EvolutionTrigger.Trade.class,
                        EvolutionTrigger.Other.class);
    }

    @Test
    void should_describe_a_level_up_with_a_minimum_level() {
        assertThat(EvolutionTrigger.LevelUp.atLevel(16).minLevel()).isEqualTo(OptionalInt.of(16));
    }

    @Test
    void should_describe_a_level_up_without_a_minimum_level() {
        // e.g. level up with high friendship: PokeAPI min_level is null
        assertThat(EvolutionTrigger.LevelUp.anyLevel().minLevel()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 16, 99, 100})
    void should_accept_a_minimum_level_between_1_and_100(int level) {
        assertThat(EvolutionTrigger.LevelUp.atLevel(level).minLevel()).hasValue(level);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101, Integer.MAX_VALUE})
    void should_reject_a_minimum_level_outside_1_to_100(int level) {
        assertThatThrownBy(() -> EvolutionTrigger.LevelUp.atLevel(level))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("evolutionTrigger"));
    }

    @Test
    void should_reject_a_null_optional_level() {
        assertThatThrownBy(() -> new EvolutionTrigger.LevelUp(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"thunder-stone", "kings-rock", "a"})
    void should_describe_using_an_item_by_its_slug(String item) {
        assertThat(new EvolutionTrigger.UseItem(item).item()).isEqualTo(item);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Thunder Stone", "thunder_stone", "<b>x</b>"})
    void should_reject_an_item_that_is_not_a_slug(String item) {
        assertThatThrownBy(() -> new EvolutionTrigger.UseItem(item))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("evolutionTrigger"));
    }

    @Test
    void should_describe_a_trade_with_no_data() {
        assertThat(new EvolutionTrigger.Trade()).isEqualTo(new EvolutionTrigger.Trade());
    }

    @ParameterizedTest
    @ValueSource(strings = {"shed", "spin", "tower-of-darkness", "three-critical-hits"})
    void should_keep_the_raw_name_of_any_other_trigger(String name) {
        assertThat(new EvolutionTrigger.Other(name).name()).isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Shed", "tower of darkness"})
    void should_reject_another_trigger_name_that_is_not_a_slug(String name) {
        assertThatThrownBy(() -> new EvolutionTrigger.Other(name))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("evolutionTrigger"));
    }

    @Test
    void should_reject_null_item_and_name() {
        assertThatThrownBy(() -> new EvolutionTrigger.UseItem(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new EvolutionTrigger.Other(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_allow_an_exhaustive_switch_over_the_kinds() {
        EvolutionTrigger trigger = new EvolutionTrigger.UseItem("thunder-stone");

        String kind = switch (trigger) {
            case EvolutionTrigger.LevelUp levelUp -> "level-up";
            case EvolutionTrigger.UseItem useItem -> "use-item";
            case EvolutionTrigger.Trade trade -> "trade";
            case EvolutionTrigger.Other other -> "other";
        };

        assertThat(kind).isEqualTo("use-item");
    }
}
