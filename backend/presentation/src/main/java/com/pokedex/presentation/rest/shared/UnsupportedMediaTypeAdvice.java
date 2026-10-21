package com.pokedex.presentation.rest.shared;

import java.net.URI;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * security.md §4.3: a write whose {@code Content-Type} is not {@code application/json} → 415
 * {@code /problems/unsupported-media-type}. Ordered ahead of {@code GlobalExceptionHandler}, whose inherited default
 * would answer with {@code about:blank}. The client's media type is not echoed.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UnsupportedMediaTypeAdvice {

    static final URI UNSUPPORTED_MEDIA_TYPE = URI.create("/problems/unsupported-media-type");

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> handle(HttpMediaTypeNotSupportedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The request body must be sent as application/json.");
        problem.setType(UNSUPPORTED_MEDIA_TYPE);
        problem.setTitle("Unsupported media type");
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
