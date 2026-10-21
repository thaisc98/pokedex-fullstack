package com.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

/**
 * Collection failures in the sealed hierarchy: {@code VersionConflictException} (409
 * /problems/version-conflict, BR-08), {@code ForbiddenException} (403 /problems/forbidden, defence in depth
 * for DELETE) and {@code NotFoundException} (404, shared with US02). Messages never echo user input.
 */
class CollectionExceptionsTest {

    @Test
    void should_permit_the_collection_exceptions_in_the_sealed_hierarchy() {
        assertThat(DomainException.class.getPermittedSubclasses())
                .contains(VersionConflictException.class, ForbiddenException.class, NotFoundException.class);
    }

    @Test
    void should_be_final_so_the_handler_switch_stays_exhaustive() {
        assertThat(Modifier.isFinal(VersionConflictException.class.getModifiers()))
                .isTrue();
        assertThat(Modifier.isFinal(ForbiddenException.class.getModifiers())).isTrue();
    }

    @Test
    void should_use_a_fixed_message_for_a_version_conflict() {
        assertThat(new VersionConflictException().getMessage())
                .isEqualTo("the resource was modified by someone else; reload and retry");
    }

    @Test
    void should_use_a_fixed_message_for_a_forbidden_operation() {
        assertThat(new ForbiddenException().getMessage()).isEqualTo("not allowed to perform this operation");
    }
}
