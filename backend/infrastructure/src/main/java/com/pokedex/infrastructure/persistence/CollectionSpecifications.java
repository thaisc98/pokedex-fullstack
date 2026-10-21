package com.pokedex.infrastructure.persistence;

import com.pokedex.domain.collection.vo.Tag;
import com.pokedex.infrastructure.persistence.entity.CollectedPokemonEntity;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;

/** UC-05 list filters as a Criteria predicate: every value is a bind parameter, never concatenated SQL. */
final class CollectionSpecifications {

    private CollectionSpecifications() {}

    static Specification<CollectedPokemonEntity> matching(@Nullable Tag tag, @Nullable String name) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>(2);
            if (tag != null) {
                Expression<Set<String>> tags = root.get("tags");
                predicates.add(cb.isMember(tag.value(), tags));
            }
            if (name != null) {
                Expression<String> entityName = root.get("name");
                predicates.add(cb.like(cb.lower(entityName), LikePattern.contains(name), LikePattern.ESCAPE));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
