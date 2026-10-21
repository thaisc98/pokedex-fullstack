package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;

/**
 * Mass as PokeAPI stores it, in hectograms (BR-02). Never negative; 0 is allowed.
 *
 * @param hectograms upstream {@code weight}
 */
public record Mass(int hectograms) {

    public Mass {
        if (hectograms < 0) {
            throw new InvalidValueException("mass", "must not be negative");
        }
    }

    /**
     * Kilograms rounded to 0.1 (69 hg → 6.9 kg). One hectogram is exactly 0.1 kg, so dividing the integer
     * by ten is exact to one decimal and always rounds the same way.
     */
    public double kilograms() {
        return hectograms / 10.0;
    }
}
