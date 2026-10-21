package com.pokedex.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pokedex.domain.collection.vo.Tag;
import com.pokedex.infrastructure.persistence.entity.CollectedPokemonEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * UC-05 filters as a parameterized Criteria predicate: exact tag membership and a case-insensitive, escaped
 * {@code LIKE} on the name. Never string-built (security.md §4.4).
 */
@SuppressWarnings("unchecked")
class CollectionSpecificationsTest {

    private final Root<CollectedPokemonEntity> root = mock(Root.class);
    private final CriteriaQuery<?> query = mock(CriteriaQuery.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    private final Path<Object> tags = mock(Path.class);
    private final Path<Object> name = mock(Path.class);
    private final Expression<String> lowerName = mock(Expression.class);

    CollectionSpecificationsTest() {
        when(root.get("tags")).thenReturn(tags);
        when(root.get("name")).thenReturn(name);
        when(cb.lower(any())).thenReturn(lowerName);
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));
    }

    @Test
    void should_match_everything_without_filters() {
        CollectionSpecifications.matching(null, null).toPredicate(root, query, cb);

        verify(cb).and();
        verify(cb, never()).like(any(), anyString(), anyChar());
        verify(cb, never()).isMember(any(String.class), any(Expression.class));
    }

    @Test
    void should_filter_by_exact_tag_membership() {
        Predicate member = mock(Predicate.class);
        when(cb.isMember(eq("starter"), any(Expression.class))).thenReturn(member);

        CollectionSpecifications.matching(new Tag("starter"), null).toPredicate(root, query, cb);

        verify(cb).and(member);
    }

    @Test
    void should_search_the_lowercased_name_with_an_escaped_pattern() {
        Predicate like = mock(Predicate.class);
        when(cb.like(lowerName, "%mon\\_\\%%", '\\')).thenReturn(like);

        CollectionSpecifications.matching(null, "mon_%").toPredicate(root, query, cb);

        verify(cb).lower(any());
        verify(cb).and(like);
    }

    @Test
    void should_combine_both_filters() {
        Predicate member = mock(Predicate.class);
        Predicate like = mock(Predicate.class);
        when(cb.isMember(eq("fire"), any(Expression.class))).thenReturn(member);
        when(cb.like(lowerName, "%char%", '\\')).thenReturn(like);

        CollectionSpecifications.matching(new Tag("fire"), "char").toPredicate(root, query, cb);

        verify(cb).and(member, like);
        assertThat(Set.of(member, like)).hasSize(2);
    }
}
