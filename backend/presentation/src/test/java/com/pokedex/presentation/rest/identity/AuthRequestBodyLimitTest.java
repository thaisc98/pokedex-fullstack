package com.pokedex.presentation.rest.identity;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.identity.port.in.AuthenticateUserUseCase;
import com.pokedex.application.identity.port.in.GetCurrentUserUseCase;
import com.pokedex.application.identity.port.in.RegisterUserUseCase;
import com.pokedex.presentation.rest.shared.StrictJsonConfig;
import com.pokedex.presentation.security.SecurityConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * security.md §4.3 / T9: the 16 KB body cap applies to every {@code /api/**} write, the public identity endpoints
 * included, and runs before the security chain and the controller.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, StrictJsonConfig.class, AuthControllerTest.Beans.class})
class AuthRequestBodyLimitTest {

    private static final String OVERSIZED = "{\"email\":\"" + "a".repeat(17 * 1024) + "\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUser;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUser;

    @MockitoBean
    private GetCurrentUserUseCase getCurrentUser;

    @ParameterizedTest(name = "POST {0} over 16 KB -> 413")
    @ValueSource(strings = {"/api/v1/auth/register", "/api/v1/auth/login"})
    void should_refuse_an_oversized_auth_body_with_413(String path) throws Exception {
        mockMvc.perform(post(path)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(OVERSIZED))
                .andExpect(status().is(413))
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("/problems/payload-too-large"))
                .andExpect(jsonPath("$.status").value(413));
        verifyNoInteractions(registerUser, authenticateUser);
    }
}
