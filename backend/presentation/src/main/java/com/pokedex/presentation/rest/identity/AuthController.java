package com.pokedex.presentation.rest.identity;

import com.pokedex.application.identity.port.in.AuthenticateUserCommand;
import com.pokedex.application.identity.port.in.AuthenticateUserUseCase;
import com.pokedex.application.identity.port.in.AuthenticationResult;
import com.pokedex.application.identity.port.in.GetCurrentUserUseCase;
import com.pokedex.application.identity.port.in.RegisterUserCommand;
import com.pokedex.application.identity.port.in.RegisterUserUseCase;
import com.pokedex.domain.identity.UserId;
import com.pokedex.domain.shared.AuthenticationRequiredException;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.presentation.rest.identity.dto.CsrfTokenDto;
import com.pokedex.presentation.rest.identity.dto.LoginRequest;
import com.pokedex.presentation.rest.identity.dto.LoginResponse;
import com.pokedex.presentation.rest.identity.dto.RegisterRequest;
import com.pokedex.presentation.rest.identity.dto.UserDto;
import com.pokedex.presentation.security.AccessTokenCookies;
import com.pokedex.presentation.security.ratelimit.AuthRateLimits;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DeferredCsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Identity endpoints (UC-06/07/08, openapi tag {@code Identity}). The OpenAPI annotations mirror
 * {@code docs/api/openapi.yaml}, checked by the contract IT. Our login and logout are not Spring's
 * {@code formLogin}/{@code logout}, so they reset the CSRF token themselves (security.md §4.2.1).
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Identity", description = "Registration, login, logout, current user and CSRF token.")
public class AuthController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final RegisterUserUseCase registerUser;
    private final AuthenticateUserUseCase authenticateUser;
    private final GetCurrentUserUseCase getCurrentUser;
    private final AuthRateLimits rateLimits;
    private final CsrfTokenRepository csrfTokens;
    private final AccessTokenCookies cookies;

    public AuthController(
            RegisterUserUseCase registerUser,
            AuthenticateUserUseCase authenticateUser,
            GetCurrentUserUseCase getCurrentUser,
            AuthRateLimits rateLimits,
            CsrfTokenRepository csrfTokens,
            AccessTokenCookies cookies) {
        this.registerUser = registerUser;
        this.authenticateUser = authenticateUser;
        this.getCurrentUser = getCurrentUser;
        this.rateLimits = rateLimits;
        this.csrfTokens = csrfTokens;
        this.cookies = cookies;
    }

    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            operationId = "registerUser",
            summary = "Register a new user",
            security = {})
    @ApiResponse(responseCode = "201", description = "The new user.")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid input.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Missing or invalid CSRF token.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "409",
            description = "Email already registered.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "413",
            description = "Request body too large.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "415",
            description = "Unsupported media type.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "429",
            description = "Too many registrations.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public UserDto register(@RequestBody RegisterRequest body, @Parameter(hidden = true) HttpServletRequest request) {
        rateLimits.registration().consume(clientIp(request));
        RegisterUserCommand command = new RegisterUserCommand(
                required(body.email(), "email"),
                required(body.password(), "password"),
                required(body.displayName(), "displayName"));
        return UserDto.from(registerUser.register(command));
    }

    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            operationId = "login",
            summary = "Log in",
            security = {})
    @ApiResponse(responseCode = "200", description = "Logged in; the token is in the access_token cookie.")
    @ApiResponse(
            responseCode = "400",
            description = "Invalid input.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Missing or invalid CSRF token.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "413",
            description = "Request body too large.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "415",
            description = "Unsupported media type.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "429",
            description = "Too many login attempts.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<LoginResponse> login(
            @RequestBody LoginRequest body,
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response) {
        String email = required(body.email(), "email");
        String password = required(body.password(), "password");
        rateLimits.login().consume(clientIp(request) + "|" + email.strip().toLowerCase(Locale.ROOT));
        AuthenticationResult result = authenticateUser.authenticate(new AuthenticateUserCommand(email, password));
        // Both cookies go straight to the servlet response: a ResponseEntity Set-Cookie header would replace the
        // CSRF reset cookie written by the repository instead of adding to it.
        csrfTokens.saveToken(null, request, response);
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                cookies.issue(result.accessToken().value()).toString());
        return ResponseEntity.ok(LoginResponse.from(result));
    }

    @PostMapping("/logout")
    @Operation(operationId = "logout", summary = "Log out")
    @ApiResponse(responseCode = "204", description = "Logged out; the access_token cookie is cleared.")
    @ApiResponse(
            responseCode = "401",
            description = "Not authenticated.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(
            responseCode = "403",
            description = "Missing or invalid CSRF token.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> logout(
            @Parameter(hidden = true) HttpServletRequest request,
            @Parameter(hidden = true) HttpServletResponse response) {
        csrfTokens.saveToken(null, request, response);
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.clear().toString());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(operationId = "getCurrentUser", summary = "Get the current user")
    @ApiResponse(responseCode = "200", description = "The current user.")
    @ApiResponse(
            responseCode = "401",
            description = "Not authenticated.",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public UserDto me(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UserId id;
        try {
            id = UserId.parse(String.valueOf(jwt.getSubject()));
        } catch (InvalidValueException e) {
            throw new AuthenticationRequiredException();
        }
        return UserDto.from(getCurrentUser.currentUser(id));
    }

    @GetMapping("/csrf")
    @Operation(
            operationId = "getCsrfToken",
            summary = "Obtain a CSRF token",
            security = {})
    @ApiResponse(responseCode = "200", description = "The CSRF token; also sets the readable XSRF-TOKEN cookie.")
    public CsrfTokenDto csrf(
            @Parameter(hidden = true) CsrfToken token, @Parameter(hidden = true) HttpServletRequest request) {
        // The SPA sends the raw cookie value in X-XSRF-TOKEN (csrf.spa()); return that same raw value, not the
        // BREACH-masked one, so the body and the cookie agree.
        Object deferred = request.getAttribute(DeferredCsrfToken.class.getName());
        CsrfToken raw = deferred instanceof DeferredCsrfToken d ? d.get() : token;
        return new CsrfTokenDto(raw.getHeaderName(), raw.getToken());
    }

    private static String required(@Nullable String value, String field) {
        if (value == null) {
            throw new InvalidValueException(field, "is required");
        }
        return value;
    }

    private static String clientIp(HttpServletRequest request) {
        return String.valueOf(request.getRemoteAddr());
    }
}
