package com.pokedex.presentation.rest.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pokedex.application.identity.port.in.AuthenticateUserCommand;
import com.pokedex.application.identity.port.in.AuthenticateUserUseCase;
import com.pokedex.application.identity.port.in.AuthenticationResult;
import com.pokedex.application.identity.port.in.GetCurrentUserUseCase;
import com.pokedex.application.identity.port.in.RegisterUserCommand;
import com.pokedex.application.identity.port.in.RegisterUserUseCase;
import com.pokedex.domain.identity.AccessToken;
import com.pokedex.domain.identity.DisplayName;
import com.pokedex.domain.identity.Email;
import com.pokedex.domain.identity.PasswordHash;
import com.pokedex.domain.identity.Role;
import com.pokedex.domain.identity.User;
import com.pokedex.domain.identity.UserId;
import com.pokedex.domain.shared.AuthenticationFailedException;
import com.pokedex.domain.shared.AuthenticationRequiredException;
import com.pokedex.domain.shared.EmailAlreadyRegisteredException;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.presentation.security.AccessTokenCookies;
import com.pokedex.presentation.security.SecurityConfig;
import com.pokedex.presentation.security.ratelimit.AuthRateLimits;
import com.pokedex.presentation.security.ratelimit.RateLimiter;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * {@code /api/v1/auth/**} slice (openapi Identity paths, security.md §3.2, §4.1–4.3): status codes, problem types,
 * cookies, CSRF reset, rate limiting and the authorization rows for the identity endpoints.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthControllerTest.Beans.class})
class AuthControllerTest {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String PASSWORD = "Pikachu-Thunder-Meadow-42";
    private static final Instant EXPIRES = Instant.parse("2026-10-21T11:00:00Z");
    private static final User ASH = new User(
            UserId.random(),
            new Email("ash@example.com"),
            new DisplayName("Ash Ketchum"),
            new PasswordHash("{bcrypt}$2a$12$abcdefghijklmnopqrstuv"),
            Role.USER,
            Instant.parse("2026-10-21T10:00:00Z"));

    @TestConfiguration(proxyBeanMethods = false)
    static class Beans {
        @Bean
        AuthRateLimits authRateLimits() {
            ConcurrentMapCache cache = new ConcurrentMapCache("rate-limit");
            return new AuthRateLimits(
                    new RateLimiter(cache, "login", 5, Duration.ofMinutes(1)),
                    new RateLimiter(cache, "register", 3, Duration.ofMinutes(1)));
        }

        @Bean
        AccessTokenCookies accessTokenCookies() {
            return new AccessTokenCookies(Duration.ofMinutes(60));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUser;

    @MockitoBean
    private AuthenticateUserUseCase authenticateUser;

    @MockitoBean
    private GetCurrentUserUseCase getCurrentUser;

    // ------------------------------------------------------------ register

    @Test
    void should_register_and_return_201_with_exactly_the_user_dto_fields() throws Exception {
        when(registerUser.register(any())).thenReturn(ASH);

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .with(remoteAddr("10.0.0.1"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("ash@example.com", PASSWORD, "Ash Ketchum")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ASH.id().value().toString()))
                .andExpect(jsonPath("$.email").value("ash@example.com"))
                .andExpect(jsonPath("$.displayName").value("Ash Ketchum"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(header().doesNotExist("Set-Cookie"));
        verify(registerUser).register(new RegisterUserCommand("ash@example.com", PASSWORD, "Ash Ketchum"));
    }

    @Test
    void should_answer_400_validation_error_for_a_missing_field() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .with(remoteAddr("10.0.0.2"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ash@example.com\",\"displayName\":\"Ash\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/validation-error"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
        verify(registerUser, never()).register(any());
    }

    @Test
    void should_reject_unknown_fields_such_as_role() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .with(remoteAddr("10.0.0.3"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@example.com\",\"password\":\"" + PASSWORD
                                + "\",\"displayName\":\"M\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/validation-error"));
        verify(registerUser, never()).register(any());
    }

    @Test
    void should_reject_malformed_json_with_a_validation_problem() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .with(remoteAddr("10.0.0.4"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/validation-error"));
    }

    @Test
    void should_map_a_weak_password_to_400_on_the_password_field() throws Exception {
        when(registerUser.register(any())).thenThrow(new InvalidValueException("password", "is too common"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .with(remoteAddr("10.0.0.5"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("ash@example.com", "password1234", "Ash")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"))
                .andExpect(content().string(not(containsString("password1234"))));
    }

    @Test
    void should_map_a_taken_email_to_409_email_taken() throws Exception {
        when(registerUser.register(any())).thenThrow(new EmailAlreadyRegisteredException());

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .with(remoteAddr("10.0.0.6"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("ash@example.com", PASSWORD, "Ash")))
                .andExpect(status().isConflict())
                .andExpect(header().string("Content-Type", PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/email-taken"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void should_limit_registrations_to_three_per_minute_per_ip() throws Exception {
        when(registerUser.register(any())).thenReturn(ASH);
        for (int i = 0; i < 3; i++) {
            register("10.0.0.7").andExpect(status().isCreated());
        }

        register("10.0.0.7")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Content-Type", PROBLEM_JSON))
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.type").value("/problems/rate-limited"))
                .andExpect(jsonPath("$.status").value(429));
        register("10.0.0.8").andExpect(status().isCreated());
    }

    // ------------------------------------------------------------ login

    @Test
    void should_log_in_set_the_cookie_reset_csrf_and_return_user_and_expiry() throws Exception {
        when(authenticateUser.authenticate(any()))
                .thenReturn(new AuthenticationResult(ASH, new AccessToken("a.b.c", EXPIRES)));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .with(remoteAddr("10.0.1.1"))
                        .cookie(new Cookie("XSRF-TOKEN", "old-token"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ash@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("ash@example.com"))
                .andExpect(jsonPath("$.user.length()").value(4))
                .andExpect(jsonPath("$.expiresAt").value("2026-10-21T11:00:00Z"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(cookie().value("access_token", "a.b.c"))
                .andExpect(cookie().httpOnly("access_token", true))
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0))
                .andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("a.b.c");
        verify(authenticateUser).authenticate(new AuthenticateUserCommand("ash@example.com", PASSWORD));
    }

    @Test
    void should_answer_401_invalid_credentials_without_a_cookie() throws Exception {
        when(authenticateUser.authenticate(any())).thenThrow(new AuthenticationFailedException());

        mockMvc.perform(login("10.0.1.2", "ash@example.com"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Content-Type", PROBLEM_JSON))
                .andExpect(header().doesNotExist("WWW-Authenticate"))
                .andExpect(jsonPath("$.type").value("/problems/invalid-credentials"))
                .andExpect(cookie().doesNotExist("access_token"));
    }

    @Test
    void should_limit_logins_to_five_per_minute_per_ip_and_email() throws Exception {
        when(authenticateUser.authenticate(any())).thenThrow(new AuthenticationFailedException());
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(login("10.0.1.3", "ash@example.com")).andExpect(status().isUnauthorized());
        }

        mockMvc.perform(login("10.0.1.3", "  ASH@example.com "))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
        mockMvc.perform(login("10.0.1.3", "misty@example.com")).andExpect(status().isUnauthorized());
    }

    @Test
    void should_answer_400_for_a_login_without_password() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ash@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void should_require_a_csrf_token_to_log_in() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ash@example.com\",\"password\":\"x\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/csrf-token-invalid"));
    }

    // ------------------------------------------------------------ me, logout, csrf

    @Test
    void should_return_the_current_user_for_a_valid_token() throws Exception {
        when(getCurrentUser.currentUser(ASH.id())).thenReturn(ASH);

        mockMvc.perform(get("/api/v1/auth/me").with(jwt().jwt(j -> j.subject(ASH.id().value().toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ash@example.com"))
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void should_answer_401_when_the_token_subject_is_not_a_user_id() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").with(jwt().jwt(j -> j.subject("not-a-uuid"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/unauthorized"));
    }

    @Test
    void should_answer_401_when_the_user_no_longer_exists() throws Exception {
        when(getCurrentUser.currentUser(any())).thenThrow(new AuthenticationRequiredException());

        mockMvc.perform(get("/api/v1/auth/me").with(jwt().jwt(j -> j.subject(ASH.id().value().toString()))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("/problems/unauthorized"));
    }

    @Test
    void should_log_out_with_204_clearing_the_cookie_and_the_csrf_token() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .with(jwt())
                        .cookie(new Cookie("XSRF-TOKEN", "old-token")))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("access_token", 0))
                .andExpect(cookie().path("access_token", "/api"))
                .andExpect(cookie().maxAge("XSRF-TOKEN", 0));
    }

    @Test
    void should_return_the_csrf_token_that_is_also_in_the_readable_cookie() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andReturn();
        String cookieValue = result.getResponse().getCookie("XSRF-TOKEN").getValue();

        assertThat(result.getResponse().getContentAsString()).contains("\"token\":\"" + cookieValue + "\"");
    }

    // ------------------------------------------------------------ authorization rows (security.md §3.2)

    @ParameterizedTest(name = "anonymous {0} {1} -> {2}")
    @CsvSource({
        "GET,  /api/v1/auth/me,          401",
        "POST, /api/v1/auth/logout,      401",
        "GET,  /api/v1/auth/csrf,        200",
        "GET,  /api/v1/auth/anything,    401",
    })
    void should_enforce_the_matrix_for_anonymous_callers(String method, String path, int expected) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                                org.springframework.http.HttpMethod.valueOf(method), path)
                        .with(csrf()))
                .andExpect(status().is(expected));
    }

    @Test
    void should_forbid_a_user_anything_not_listed() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist").with(jwt()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/forbidden"));
    }

    private org.springframework.test.web.servlet.ResultActions register(String ip) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/register")
                .with(csrf())
                .with(remoteAddr(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("ash@example.com", PASSWORD, "Ash")));
    }

    private static org.springframework.test.web.servlet.RequestBuilder login(String ip, String email) {
        return post("/api/v1/auth/login")
                .with(csrf())
                .with(remoteAddr(ip))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Wrong-Password-1\"}");
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor remoteAddr(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private static org.springframework.test.web.servlet.result.ContentResultMatchers content() {
        return org.springframework.test.web.servlet.result.MockMvcResultMatchers.content();
    }

    private static String json(String email, String password, String displayName) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"displayName\":\"" + displayName
                + "\"}";
    }
}
