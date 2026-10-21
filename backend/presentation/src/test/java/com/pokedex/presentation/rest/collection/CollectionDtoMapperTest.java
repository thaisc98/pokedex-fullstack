package com.pokedex.presentation.rest.collection;

import static com.pokedex.presentation.rest.collection.CollectionFixtures.CUSTOM;
import static com.pokedex.presentation.rest.collection.CollectionFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.collection.vo.Coordinates;
import com.pokedex.domain.collection.vo.GeoMetadata;
import com.pokedex.domain.collection.vo.LocalizedName;
import com.pokedex.domain.collection.vo.Tag;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import com.pokedex.presentation.rest.catalog.dto.AbilityDto;
import com.pokedex.presentation.rest.catalog.dto.BaseStatDto;
import com.pokedex.presentation.rest.collection.dto.CollectedPokemonDto;
import com.pokedex.presentation.rest.collection.dto.CoordinatesDto;
import com.pokedex.presentation.rest.collection.dto.CustomAttributesDto;
import com.pokedex.presentation.rest.collection.dto.GeoMetadataDto;
import com.pokedex.presentation.rest.collection.dto.LocalizedNameDto;
import com.pokedex.presentation.rest.shared.PageResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * REST DTOs ↔ domain for the collection. Requests become domain value objects; any rule failure is re-reported
 * on the request's Spring {@code FieldError}-style path ({@code customAttributes.tags[2]}).
 */
class CollectionDtoMapperTest {

    @Test
    void should_map_every_field_of_the_aggregate_to_the_dto() {
        CollectedPokemonDto dto = CollectionDtoMapper.toDto(pikachu(CUSTOM, 3));

        assertThat(dto.id()).isEqualTo(25);
        assertThat(dto.version()).isEqualTo(3);
        assertThat(dto.syncedAt()).isEqualTo(CollectionFixtures.SYNCED);
        assertThat(dto.updatedAt()).isEqualTo(CollectionFixtures.UPDATED);
        assertThat(dto.snapshot().id()).isEqualTo(25);
        assertThat(dto.snapshot().name()).isEqualTo("pikachu");
        assertThat(dto.snapshot().sprite()).isEqualTo(CollectionFixtures.SPRITE);
        assertThat(dto.snapshot().category()).isEqualTo("Mouse Pokémon");
        assertThat(dto.snapshot().mass()).isEqualTo(6.0);
        assertThat(dto.snapshot().abilities())
                .containsExactly(new AbilityDto("static", false), new AbilityDto("lightning-rod", true));
        assertThat(dto.snapshot().stats()).containsExactly(new BaseStatDto("hp", 35), new BaseStatDto("speed", 90));
        assertThat(dto.custom().localizedNames()).containsExactly(new LocalizedNameDto("ja", "ピカチュウ"));
        assertThat(dto.custom().geo())
                .isEqualTo(new GeoMetadataDto("Kanto", "forest", new CoordinatesDto(35.6, 139.7)));
        assertThat(dto.custom().tags()).containsExactly("starter", "electric");
    }

    @Test
    void should_map_absent_geo_and_coordinates_to_null() {
        CollectedPokemonDto empty = CollectionDtoMapper.toDto(pikachu(CustomAttributes.EMPTY, 0));
        CollectedPokemonDto noCoordinates = CollectionDtoMapper.toDto(
                pikachu(CustomAttributes.of(List.of(), new GeoMetadata("Kanto", null, null), List.of()), 0));

        assertThat(empty.custom().geo()).isNull();
        assertThat(empty.custom().localizedNames()).isEmpty();
        assertThat(empty.custom().tags()).isEmpty();
        assertThat(noCoordinates.custom().geo()).isEqualTo(new GeoMetadataDto("Kanto", null, null));
    }

    @Test
    void should_map_a_page_to_the_contract_envelope() {
        PageResponse<CollectedPokemonDto> response =
                CollectionDtoMapper.toResponse(new Page<>(List.of(pikachu(CUSTOM, 0)), new PageRequest(1, 1), 3));

        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(1);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void should_build_custom_attributes_from_a_request() {
        CustomAttributesDto request = new CustomAttributesDto(
                List.of(new LocalizedNameDto("ja", "ピカチュウ")),
                new GeoMetadataDto("Kanto", "forest", new CoordinatesDto(-90.0, 180.0)),
                List.of("  Fire-Type ", "fire-type", "STARTER"));

        CustomAttributes custom = CollectionDtoMapper.toCustomAttributes(request);

        assertThat(custom.localizedNames()).containsExactly(new LocalizedName("ja", "ピカチュウ"));
        assertThat(custom.geo()).isEqualTo(new GeoMetadata("Kanto", "forest", new Coordinates(-90, 180)));
        assertThat(custom.tags()).containsExactly(new Tag("fire-type"), new Tag("starter"));
    }

    @Test
    void should_accept_a_request_without_geo() {
        CustomAttributes custom =
                CollectionDtoMapper.toCustomAttributes(new CustomAttributesDto(List.of(), null, List.of()));

        assertThat(custom).isEqualTo(CustomAttributes.EMPTY);
    }

    @Test
    void should_report_an_invalid_tag_on_its_index() {
        assertField(
                new CustomAttributesDto(List.of(), null, List.of("ok", "fine", "Fire Type!")),
                "customAttributes.tags[2]");
    }

    @Test
    void should_report_too_many_tags_on_the_list() {
        List<String> eleven = IntStream.range(0, 11).mapToObj(i -> "t-" + i).toList();

        assertField(new CustomAttributesDto(List.of(), null, eleven), "customAttributes.tags");
    }

    @Test
    void should_report_a_null_tag_on_its_index() {
        assertField(new CustomAttributesDto(List.of(), null, Arrays.asList("ok", null)), "customAttributes.tags[1]");
    }

    @Test
    void should_report_an_invalid_language_on_its_index() {
        assertField(
                new CustomAttributesDto(List.of(new LocalizedNameDto("not a language", "X")), null, List.of()),
                "customAttributes.localizedNames[0].lang");
    }

    @Test
    void should_report_an_invalid_localized_value_on_its_index() {
        assertField(
                new CustomAttributesDto(
                        List.of(new LocalizedNameDto("en", "ok"), new LocalizedNameDto("fr", "   ")), null, List.of()),
                "customAttributes.localizedNames[1].value");
    }

    @Test
    void should_report_missing_lang_or_value_on_their_path() {
        assertField(
                new CustomAttributesDto(List.of(new LocalizedNameDto(null, "X")), null, List.of()),
                "customAttributes.localizedNames[0].lang");
        assertField(
                new CustomAttributesDto(List.of(new LocalizedNameDto("en", null)), null, List.of()),
                "customAttributes.localizedNames[0].value");
        assertField(
                new CustomAttributesDto(new ArrayList<>(Arrays.asList((LocalizedNameDto) null)), null, List.of()),
                "customAttributes.localizedNames[0]");
    }

    @Test
    void should_report_two_names_for_one_language_on_the_list() {
        assertField(
                new CustomAttributesDto(
                        List.of(new LocalizedNameDto("ja", "A"), new LocalizedNameDto("ja", "B")), null, List.of()),
                "customAttributes.localizedNames");
    }

    @Test
    void should_report_geo_violations_on_their_path() {
        assertField(geo(new GeoMetadataDto("r".repeat(61), null, null)), "customAttributes.geo.region");
        assertField(geo(new GeoMetadataDto(null, "   ", null)), "customAttributes.geo.habitat");
        assertField(
                geo(new GeoMetadataDto(null, null, new CoordinatesDto(91.0, 0.0))),
                "customAttributes.geo.coordinates.latitude");
        assertField(
                geo(new GeoMetadataDto(null, null, new CoordinatesDto(0.0, 180.5))),
                "customAttributes.geo.coordinates.longitude");
        assertField(
                geo(new GeoMetadataDto(null, null, new CoordinatesDto(null, 0.0))),
                "customAttributes.geo.coordinates.latitude");
        assertField(
                geo(new GeoMetadataDto(null, null, new CoordinatesDto(0.0, null))),
                "customAttributes.geo.coordinates.longitude");
    }

    @Test
    void should_require_the_localized_names_and_tags_lists() {
        assertField(new CustomAttributesDto(null, null, List.of()), "customAttributes.localizedNames");
        assertField(new CustomAttributesDto(List.of(), null, null), "customAttributes.tags");
    }

    @Test
    void should_not_echo_rejected_input_in_messages() {
        assertThatThrownBy(() -> CollectionDtoMapper.toCustomAttributes(
                        new CustomAttributesDto(List.of(), null, List.of("<script>evil</script>"))))
                .isInstanceOf(InvalidValueException.class)
                .message()
                .doesNotContain("evil");
    }

    private static CustomAttributesDto geo(GeoMetadataDto geo) {
        return new CustomAttributesDto(List.of(), geo, List.of());
    }

    private static void assertField(CustomAttributesDto request, String field) {
        assertThatThrownBy(() -> CollectionDtoMapper.toCustomAttributes(request))
                .isInstanceOfSatisfying(
                        InvalidValueException.class, e -> assertThat(e.field()).isEqualTo(field));
    }
}
