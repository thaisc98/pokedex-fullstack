package com.pokedex.application.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.application.collection.command.UpdateCustomAttributesCommand;
import com.pokedex.application.collection.port.in.UpdateCollectedPokemonUseCase;
import com.pokedex.application.collection.service.UpdateCollectedPokemonService;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CollectedPokemon;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.collection.vo.CollectedPokemonId;
import com.pokedex.domain.collection.vo.Coordinates;
import com.pokedex.domain.collection.vo.GeoMetadata;
import com.pokedex.domain.collection.vo.LocalizedName;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.VersionConflictException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** UC-04 {@code UpdateCollectedPokemon}: full replace of custom attributes (BR-06), version check (BR-08). */
class UpdateCollectedPokemonServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-21T10:00:00Z");
    private static final PokedexId PIKACHU = new PokedexId(25);
    private static final CustomAttributes CUSTOM = CustomAttributes.of(
            List.of(new LocalizedName("ja", "ピカチュウ")),
            new GeoMetadata("Kanto", "forest", new Coordinates(35.0, 139.0)),
            List.of("starter", "electric"));

    private final MutableClock clock = new MutableClock(T0);
    private final InMemoryCollectedPokemonRepository repository = new InMemoryCollectedPokemonRepository();
    private final UpdateCollectedPokemonUseCase useCase = new UpdateCollectedPokemonService(repository, clock);

    @BeforeEach
    void seed() {
        repository.save(CollectedPokemon.collect(
                CollectedPokemonId.random(), CollectionFixtures.snapshot(25, "pikachu"), clock));
        clock.advance(Duration.ofMinutes(5));
    }

    @Test
    void should_replace_custom_attributes_and_increment_version() {
        CollectedPokemon updated = useCase.update(new UpdateCustomAttributesCommand(PIKACHU, CUSTOM, 0));

        assertThat(updated.custom()).isEqualTo(CUSTOM);
        assertThat(updated.version()).isEqualTo(1);
        assertThat(updated.updatedAt()).isEqualTo(T0.plus(Duration.ofMinutes(5)));
        assertThat(updated.syncedAt()).isEqualTo(T0);
        assertThat(repository.findByPokedexId(PIKACHU).orElseThrow().custom()).isEqualTo(CUSTOM);
    }

    @Test
    void should_never_change_the_upstream_snapshot() {
        CollectedPokemon updated = useCase.update(new UpdateCustomAttributesCommand(PIKACHU, CUSTOM, 0));

        assertThat(updated.snapshot()).isEqualTo(CollectionFixtures.snapshot(25, "pikachu"));
    }

    @Test
    void should_reject_a_stale_version_with_a_conflict_and_keep_the_stored_record() {
        useCase.update(new UpdateCustomAttributesCommand(PIKACHU, CUSTOM, 0));

        assertThatThrownBy(() -> useCase.update(new UpdateCustomAttributesCommand(PIKACHU, CustomAttributes.EMPTY, 0)))
                .isInstanceOf(VersionConflictException.class);

        CollectedPokemon stored = repository.findByPokedexId(PIKACHU).orElseThrow();
        assertThat(stored.custom()).isEqualTo(CUSTOM);
        assertThat(stored.version()).isEqualTo(1);
    }

    @Test
    void should_accept_consecutive_updates_that_carry_the_latest_version() {
        CollectedPokemon first = useCase.update(new UpdateCustomAttributesCommand(PIKACHU, CUSTOM, 0));

        CollectedPokemon second =
                useCase.update(new UpdateCustomAttributesCommand(PIKACHU, CustomAttributes.EMPTY, first.version()));

        assertThat(second.version()).isEqualTo(2);
        assertThat(second.custom()).isEqualTo(CustomAttributes.EMPTY);
    }

    @Test
    void should_throw_not_found_when_the_record_does_not_exist() {
        assertThatThrownBy(() -> useCase.update(new UpdateCustomAttributesCommand(new PokedexId(26), CUSTOM, 0)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void should_reject_an_invalid_command() {
        assertThatThrownBy(() -> new UpdateCustomAttributesCommand(null, CUSTOM, 0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdateCustomAttributesCommand(PIKACHU, null, 0))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdateCustomAttributesCommand(PIKACHU, CUSTOM, -1))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void should_reject_nulls() {
        assertThatThrownBy(() -> useCase.update(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdateCollectedPokemonService(null, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new UpdateCollectedPokemonService(repository, (Clock) null))
                .isInstanceOf(NullPointerException.class);
    }
}
