package com.pokedex.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.catalog.vo.PokemonName;
import org.junit.jupiter.api.Test;

/** {@code PokemonNameRef}: one entry of the name index (id + name). */
class PokemonNameRefTest {

    @Test
    void should_keep_id_and_name() {
        PokemonNameRef ref = new PokemonNameRef(new PokedexId(25), new PokemonName("pikachu"));

        assertThat(ref.id().value()).isEqualTo(25);
        assertThat(ref.name().value()).isEqualTo("pikachu");
    }

    @Test
    void should_reject_null_parts() {
        assertThatThrownBy(() -> new PokemonNameRef(null, new PokemonName("pikachu")))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PokemonNameRef(new PokedexId(25), null)).isInstanceOf(NullPointerException.class);
    }
}
