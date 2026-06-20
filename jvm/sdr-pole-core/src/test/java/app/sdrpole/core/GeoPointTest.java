package app.sdrpole.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeoPointTest {
    @Test void calculatesKnownDistance() {
        var chicago = new GeoPoint(41.8781, -87.6298);
        var milwaukee = new GeoPoint(43.0389, -87.9065);
        assertEquals(130.7, chicago.distanceKmTo(milwaukee), 1.0);
    }

    @Test void rejectsInvalidCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(91, 0));
    }
}
