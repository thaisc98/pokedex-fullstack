package com.pokedex.domain.catalog.vo;

import com.pokedex.domain.shared.InvalidValueException;
import java.util.Objects;

/** One of the six core statistics with its base value (1–255). */
public record BaseStat(StatName name, int value) {

    public static final int MIN = 1;
    public static final int MAX = 255;

    public BaseStat {
        Objects.requireNonNull(name, "name");
        if (value < MIN || value > MAX) {
            throw new InvalidValueException("stat", "must be between 1 and 255");
        }
    }
}
