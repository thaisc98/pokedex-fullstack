package com.pokedex.presentation.rest.collection;

import static com.pokedex.presentation.rest.collection.CollectionFixtures.CUSTOM;
import static com.pokedex.presentation.rest.collection.CollectionFixtures.pikachu;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.collection.command.CollectionQuery;
import com.pokedex.application.collection.command.UpdateCustomAttributesCommand;
import com.pokedex.application.collection.port.in.DeleteCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.GetCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.ListCollectedPokemonUseCase;
import com.pokedex.application.collection.port.in.SyncPokemonUseCase;
import com.pokedex.application.collection.port.in.SyncResult;
import com.pokedex.application.collection.port.in.UpdateCollectedPokemonUseCase;
import com.pokedex.domain.catalog.vo.PokedexId;
import com.pokedex.domain.collection.CustomAttributes;
import com.pokedex.domain.collection.vo.Tag;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.shared.Page;
import com.pokedex.domain.shared.PageRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code /api/v1/collection} slice without the security filter chain (the access rules, CSRF and 401/403 are
 * covered by the security slice and the authorization-matrix tests): status codes, Location, DTO shape,
 * request → command mapping, validation → 400 on the right field, 415.
 */
@WebMvcTest(CollectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class CollectionControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final TestingAuthenticationToken USER = new TestingAuthenticationToken("u", "x", "ROLE_USER");
    private static final TestingAuthenticationToken ADMIN =
            new TestingAuthenticationToken("a", "x", "ROLE_USER", "ROLE_ADMIN");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SyncPokemonUseCase sync;

    @MockitoBean
    private ListCollectedPokemonUseCase list;

    @MockitoBean
    private GetCollectedPokemonUseCase getOne;

    @MockitoBean
    private UpdateCollectedPokemonUseCase update;

    @MockitoBean
    private DeleteCollectedPokemonUseCase deleteOne;

    // ------------------------------------------------------------ POST (sync)

    @Test
    void should_return_201_with_location_on_first_sync() throws Exception {
        when(sync.sync(new PokedexId(25))).thenReturn(new SyncResult(pikachu(CustomAttributes.EMPTY, 0), true));

        mockMvc.perform(post("/api/v1/collection")
                        .principal(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pokedexId\":25}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/collection/25"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.snapshot.mass").value(6.0))
                .andExpect(jsonPath("$.snapshot.stats[0].name").value("hp"))
                .andExpect(jsonPath("$.custom.tags").isEmpty())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.syncedAt").value("2026-10-21T10:00:00.123456Z"));
    }

    @Test
    void should_return_200_without_location_on_resync() throws Exception {
        when(sync.sync(new PokedexId(25))).thenReturn(new SyncResult(pikachu(CUSTOM, 2), false));

        mockMvc.perform(post("/api/v1/collection")
                        .principal(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pokedexId\":25}"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.custom.tags[0]").value("starter"));
    }

    @Test
    void should_reject_a_sync_body_without_pokedex_id() throws Exception {
        mockMvc.perform(post("/api/v1/collection")
                        .principal(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("pokedexId"));
        verifyNoInteractions(sync);
    }

    @Test
    void should_reject_a_non_positive_pokedex_id() throws Exception {
        mockMvc.perform(post("/api/v1/collection")
                        .principal(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pokedexId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("pokedexId"));
        verifyNoInteractions(sync);
    }

    @Test
    void should_reject_a_non_json_sync_with_415_problem() throws Exception {
        mockMvc.perform(post("/api/v1/collection")
                        .principal(USER)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("{\"pokedexId\":25}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/unsupported-media-type"))
                .andExpect(jsonPath("$.status").value(415));
        verifyNoInteractions(sync);
    }

    // ------------------------------------------------------------ GET list

    @Test
    void should_list_with_defaults_and_no_filters() throws Exception {
        when(list.list(CollectionQuery.firstPage()))
                .thenReturn(new Page<>(List.of(pikachu(CUSTOM, 0)), PageRequest.ofPage(0), 1));

        mockMvc.perform(get("/api/v1/collection").principal(USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].id").value(25));
    }

    @Test
    void should_pass_page_tag_and_q_to_the_use_case() throws Exception {
        CollectionQuery expected = new CollectionQuery(new PageRequest(2, 5), new Tag("starter"), "pika");
        when(list.list(expected)).thenReturn(new Page<>(List.of(), new PageRequest(2, 5), 0));

        mockMvc.perform(get("/api/v1/collection")
                        .principal(USER)
                        .param("page", "2")
                        .param("size", "5")
                        .param("tag", "starter")
                        .param("q", "Pika"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void should_reject_invalid_list_parameters_on_their_field() throws Exception {
        for (String[] param : new String[][] {
            {"size", "0"}, {"size", "101"}, {"page", "-1"}, {"page", "abc"}, {"tag", "Fire Type!"}, {"q", ""}
        }) {
            mockMvc.perform(get("/api/v1/collection").principal(USER).param(param[0], param[1]))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.errors[0].field").value(param[0]));
        }
        verifyNoInteractions(list);
    }

    // ------------------------------------------------------------ GET one

    @Test
    void should_get_one_by_pokedex_id() throws Exception {
        when(getOne.get(new PokedexId(25))).thenReturn(pikachu(CUSTOM, 4));

        mockMvc.perform(get("/api/v1/collection/25").principal(USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(25))
                .andExpect(jsonPath("$.version").value(4))
                .andExpect(jsonPath("$.custom.geo.coordinates.latitude").value(35.6));
    }

    @Test
    void should_reject_an_invalid_path_id_on_field_pokedex_id() throws Exception {
        for (String id : List.of("0", "-1", "abc")) {
            mockMvc.perform(get("/api/v1/collection/" + id).principal(USER))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                    .andExpect(jsonPath("$.errors[0].field").value("pokedexId"));
        }
        verifyNoInteractions(getOne);
    }

    // ------------------------------------------------------------ PUT

    @Test
    void should_map_the_update_request_to_a_command() throws Exception {
        when(update.update(any())).thenReturn(pikachu(CUSTOM, 5));

        mockMvc.perform(put("/api/v1/collection/25")
                        .principal(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customAttributes":{"localizedNames":[{"lang":"ja","value":"ピカチュウ"}],
                                 "geo":{"region":"Kanto","habitat":"forest",
                                        "coordinates":{"latitude":35.6,"longitude":139.7}},
                                 "tags":["Starter","electric"]},
                                 "version":4}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(5));

        ArgumentCaptor<UpdateCustomAttributesCommand> command =
                ArgumentCaptor.forClass(UpdateCustomAttributesCommand.class);
        verify(update).update(command.capture());
        assertThat(command.getValue().pokedexId()).isEqualTo(new PokedexId(25));
        assertThat(command.getValue().expectedVersion()).isEqualTo(4);
        assertThat(command.getValue().customAttributes()).isEqualTo(CUSTOM);
    }

    @Test
    void should_require_custom_attributes_and_version() throws Exception {
        mockMvc.perform(put("/api/v1/collection/25")
                        .principal(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("customAttributes"));
        mockMvc.perform(put("/api/v1/collection/25")
                        .principal(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customAttributes\":{\"localizedNames\":[],\"tags\":[]}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("version"));
        verify(update, never()).update(any());
    }

    @Test
    void should_report_an_invalid_tag_on_its_request_path() throws Exception {
        mockMvc.perform(put("/api/v1/collection/25")
                        .principal(USER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customAttributes\":{\"localizedNames\":[],\"tags\":[\"ok\",\"Fire Type!\"]},"
                                + "\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("customAttributes.tags[1]"));
        verify(update, never()).update(any());
    }

    @Test
    void should_reject_a_non_json_update_with_415() throws Exception {
        mockMvc.perform(put("/api/v1/collection/25")
                        .principal(USER)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.type").value("/problems/unsupported-media-type"));
    }

    // ------------------------------------------------------------ DELETE

    @Test
    void should_pass_admin_role_to_the_use_case_and_return_204() throws Exception {
        mockMvc.perform(delete("/api/v1/collection/25").principal(ADMIN)).andExpect(status().isNoContent());

        verify(deleteOne).delete(new PokedexId(25), Role.ADMIN);
    }

    @Test
    void should_pass_user_role_to_the_use_case_for_a_non_admin() throws Exception {
        mockMvc.perform(delete("/api/v1/collection/25").principal(USER)).andExpect(status().isNoContent());

        verify(deleteOne).delete(new PokedexId(25), Role.USER);
    }

    @Test
    void should_reject_an_invalid_path_id_on_delete() throws Exception {
        mockMvc.perform(delete("/api/v1/collection/0").principal(ADMIN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("pokedexId"));
        verifyNoInteractions(deleteOne);
    }
}
