package com.pokedex.presentation.shared;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.shared.SampleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Surefire slice test proving the webmvc test stack plus {@code @MockitoBean} run on Java 25.
 */
@WebMvcTest(SampleController.class)
class SampleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SampleService service;

    @Test
    void should_return_the_doubled_value_as_json() throws Exception {
        when(service.doubled()).thenReturn(42);

        mockMvc.perform(get("/sample"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").value(42));
    }
}
