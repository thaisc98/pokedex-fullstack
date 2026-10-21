package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;

/**
 * Height as PokeAPI stores it, in decimetres. Never negative; 0 is allowed.
 *
 * @param decimetres upstream {@code height}
 */
public record Height(int decimetres) {

    public Height {
        if (decimetres < 0) {
            throw new InvalidValueException("height", "must not be negative");
        }
    }

    /** Meters (4 dm → 0.4 m); exact to one decimal. */
    public double meters() {
        return decimetres / 10.0;
    }
}
