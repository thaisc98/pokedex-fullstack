package com.pokedex.presentation.rest.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.pokedex.domain.shared.ForbiddenException;
import com.pokedex.domain.shared.VersionConflictException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

/** Direct unit tests for mappings that the controller slice cannot reach. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void should_report_a_type_mismatch_without_a_property_name_against_the_request() {
        TypeMismatchException mismatch = new TypeMismatchException("abc", Integer.class);

        ResponseEntity<Object> response = handler.handleTypeMismatch(
                mismatch,
                HttpHeaders.EMPTY,
                HttpStatus.BAD_REQUEST,
                new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail problem = (ProblemDetail) response.getBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getType()).isEqualTo(GlobalExceptionHandler.VALIDATION_ERROR);
        assertThat(problem.getProperties())
                .containsEntry(
                        "errors", List.of(new GlobalExceptionHandler.FieldViolation("request", "must be an integer")));
    }

    @Test
    void should_map_a_version_conflict_to_409_with_its_problem_type() {
        ResponseEntity<ProblemDetail> response = handler.handleDomain(new VersionConflictException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail problem = response.getBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getType()).isEqualTo(java.net.URI.create("/problems/version-conflict"));
        assertThat(problem.getStatus()).isEqualTo(409);
    }

    @Test
    void should_map_a_forbidden_operation_to_403_with_its_problem_type() {
        ResponseEntity<ProblemDetail> response = handler.handleDomain(new ForbiddenException());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        ProblemDetail problem = response.getBody();
        assertThat(problem).isNotNull();
        assertThat(problem.getType()).isEqualTo(java.net.URI.create("/problems/forbidden"));
        assertThat(problem.getStatus()).isEqualTo(403);
    }
}
