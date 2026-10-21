package com.pokedex.presentation.rest.error;

import com.pokedex.domain.shared.AuthenticationFailedException;
import com.pokedex.domain.shared.AuthenticationRequiredException;
import com.pokedex.domain.shared.DomainException;
import com.pokedex.domain.shared.EmailAlreadyRegisteredException;
import com.pokedex.domain.shared.ForbiddenException;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.NotFoundException;
import com.pokedex.domain.shared.UpstreamUnavailableException;
import com.pokedex.domain.shared.VersionConflictException;
import com.pokedex.presentation.security.ratelimit.RateLimitedException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every error becomes an RFC 9457 {@link ProblemDetail} (security.md §4.6). The sealed {@link DomainException}
 * hierarchy is mapped with an exhaustive {@code switch}: a new subtype fails compilation until it is mapped.
 * Messages are written here; exception messages, class names and stack traces never reach the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final URI VALIDATION_ERROR = URI.create("/problems/validation-error");
    static final URI NOT_FOUND = URI.create("/problems/not-found");
    static final URI UPSTREAM_UNAVAILABLE = URI.create("/problems/upstream-unavailable");
    static final URI INTERNAL_ERROR = URI.create("/problems/internal-error");
    static final URI UNAUTHORIZED = URI.create("/problems/unauthorized");
    static final URI INVALID_CREDENTIALS = URI.create("/problems/invalid-credentials");
    static final URI EMAIL_TAKEN = URI.create("/problems/email-taken");
    static final URI RATE_LIMITED = URI.create("/problems/rate-limited");
    static final URI VERSION_CONFLICT = URI.create("/problems/version-conflict");
    static final URI FORBIDDEN = URI.create("/problems/forbidden");

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** One field-level violation in {@code errors[]} (openapi {@code FieldViolation}). */
    public record FieldViolation(String field, String message) {}

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> handleDomain(DomainException exception) {
        return switch (exception) {
            case InvalidValueException invalid ->
                validationProblem(new FieldViolation(invalid.field(), invalid.getMessage()));
            case EmailAlreadyRegisteredException taken ->
                problem(
                        HttpStatus.CONFLICT,
                        EMAIL_TAKEN,
                        "Email already registered",
                        "An account with this email already exists.");
            // One fixed body for every login failure: nothing reveals which emails exist (security.md §4.1).
            case AuthenticationFailedException failed ->
                problem(
                        HttpStatus.UNAUTHORIZED,
                        INVALID_CREDENTIALS,
                        "Invalid credentials",
                        "The email or password is incorrect.");
            case AuthenticationRequiredException required ->
                problem(
                        HttpStatus.UNAUTHORIZED,
                        UNAUTHORIZED,
                        "Unauthorized",
                        "Authentication is required to access this resource.");
            // BR-08: someone else changed the record first; the client reloads and retries.
            case VersionConflictException conflict ->
                problem(
                        HttpStatus.CONFLICT,
                        VERSION_CONFLICT,
                        "Version conflict",
                        "The resource was modified by someone else. Reload it and try again.");
            // BR-10, defence in depth: the use case refused the caller's role.
            case ForbiddenException forbidden ->
                problem(HttpStatus.FORBIDDEN, FORBIDDEN, "Forbidden", "You are not allowed to perform this operation.");
            case NotFoundException notFound -> {
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                        HttpStatus.NOT_FOUND, "The requested resource does not exist.");
                problem.setType(NOT_FOUND);
                problem.setTitle("Not found");
                yield ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
            }
            case UpstreamUnavailableException unavailable -> {
                String correlationId = correlationId();
                LOG.warn("Upstream unavailable [correlationId={}]", correlationId, unavailable);
                ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "The Pokémon catalog is temporarily unavailable. Please try again later.");
                problem.setType(UPSTREAM_UNAVAILABLE);
                problem.setTitle("Upstream service unavailable");
                problem.setProperty("correlationId", correlationId);
                yield ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(problem);
            }
        };
    }

    /** security.md §4.1: too many login or registration attempts → 429 with {@code Retry-After} (seconds). */
    @ExceptionHandler(RateLimitedException.class)
    ResponseEntity<ProblemDetail> handleRateLimited(RateLimitedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please wait and try again.");
        problem.setType(RATE_LIMITED);
        problem.setTitle("Too many requests");
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.retryAfterSeconds()))
                .body(problem);
    }

    /** Last resort: a generic 500. Details go to the log only, linked by the correlation id. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        String correlationId = correlationId();
        LOG.error("Unexpected error [correlationId={}]", correlationId, exception);
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        problem.setType(INTERNAL_ERROR);
        problem.setTitle("Internal server error");
        problem.setProperty("correlationId", correlationId);
        return ResponseEntity.internalServerError().body(problem);
    }

    /** {@code ?page=abc}: a query parameter that is not a valid integer. The rejected value is not echoed. */
    @Override
    protected @Nullable ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = ex.getPropertyName() != null ? ex.getPropertyName() : "request";
        ResponseEntity<ProblemDetail> response = validationProblem(new FieldViolation(field, "must be an integer"));
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    /**
     * Malformed JSON or an unknown field (mass assignment, security.md §4.3). The parser message is not echoed: it
     * can contain the rejected input.
     */
    @Override
    protected @Nullable ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ResponseEntity<ProblemDetail> response =
                validationProblem(new FieldViolation("body", "must be a JSON object with only the documented fields"));
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, URI type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        return ResponseEntity.status(status).body(problem);
    }

    private static ResponseEntity<ProblemDetail> validationProblem(FieldViolation violation) {
        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "One or more fields are invalid.");
        problem.setType(VALIDATION_ERROR);
        problem.setTitle("Validation failed");
        problem.setProperty("errors", List.of(violation));
        return ResponseEntity.badRequest().body(problem);
    }

    private static String correlationId() {
        return UUID.randomUUID().toString();
    }
}
