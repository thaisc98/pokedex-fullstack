package com.pokedex.presentation.rest.error;

import com.pokedex.domain.shared.DomainException;
import com.pokedex.domain.shared.InvalidValueException;
import com.pokedex.domain.shared.UpstreamUnavailableException;
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
    static final URI UPSTREAM_UNAVAILABLE = URI.create("/problems/upstream-unavailable");
    static final URI INTERNAL_ERROR = URI.create("/problems/internal-error");

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** One field-level violation in {@code errors[]} (openapi {@code FieldViolation}). */
    public record FieldViolation(String field, String message) {}

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> handleDomain(DomainException exception) {
        return switch (exception) {
            case InvalidValueException invalid ->
                validationProblem(new FieldViolation(invalid.field(), invalid.getMessage()));
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
