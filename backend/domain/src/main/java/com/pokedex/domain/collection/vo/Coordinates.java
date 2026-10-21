package com.pokedex.domain.collection.vo;

import com.pokedex.domain.shared.InvalidValueException;

/** Latitude -90..90 and longitude -180..180, both inclusive and finite. */
public record Coordinates(double latitude, double longitude) {

    public Coordinates {
        if (!(latitude >= -90 && latitude <= 90)) {
            throw new InvalidValueException("coordinates.latitude", "must be between -90 and 90");
        }
        if (!(longitude >= -180 && longitude <= 180)) {
            throw new InvalidValueException("coordinates.longitude", "must be between -180 and 180");
        }
    }
}
