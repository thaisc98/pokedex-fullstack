package com.pokedex.presentation.rest.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.identity.port.in.AuthenticateUserUseCase;
import com.pokedex.application.identity.port.in.GetCurrentUserUseCase;
import com.pokedex.application.identity.port.in.RegisterUserUseCase;
import com.pokedex.presentation.rest.shared.StrictJsonConfig;
import com.pokedex.presentation.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * {@code GET /api/v1/auth/csrf} (security.md §4.2.1) with the real {@code csrf.spa()} cookie repository. A separate
 * class on purpose: spring-security-test's {@code csrf()} swaps the shared filter's repository for a test one, so
 * this context must never use it.
 */
@WebMvcTest(controllers = AuthController.class, properties = "pokedex.test.context=csrf-endpoint")
@Import({SecurityConfig.class, StrictJsonConfig.class, AuthControllerTest.Beans.class})
class CsrfEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUser;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUser;

    @MockitoBean
    private GetCurrentUserUseCase getCurrentUser;

    @Test
    void should_return_the_csrf_token_that_is_also_in_the_readable_cookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andReturn();
        String cookieValue = result.getResponse().getCookie("XSRF-TOKEN").getValue();

        assertThat(cookieValue).isNotBlank();
        assertThat(result.getResponse().getContentAsString()).contains("\"token\":\"" + cookieValue + "\"");
    }
}
