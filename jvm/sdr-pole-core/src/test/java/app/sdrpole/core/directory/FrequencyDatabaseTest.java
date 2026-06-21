package app.sdrpole.core.directory;

import app.sdrpole.core.DemodulationMode;
import app.sdrpole.core.GeoPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FrequencyDatabaseTest {
    @TempDir Path temporary;

    @Test void migratesSeedsUpsertsAndFiltersByDistance() throws Exception {
        var database = new FrequencyDatabase(temporary.resolve("nested/frequencies.sqlite3"));
        database.initialize();
        assertEquals(7, database.countsBySource().get("noaa-nwr"));

        database.upsert(List.of(
                channel("near", new GeoPoint(41.88, -87.63)),
                channel("far", new GeoPoint(34.05, -118.24))));
        var nearby = database.channelsNear(new GeoPoint(41.88, -87.63), 50, 20);
        assertTrue(nearby.stream().anyMatch(item -> item.externalId().equals("near")));
        assertFalse(nearby.stream().anyMatch(item -> item.externalId().equals("far")));
        assertEquals(7, nearby.stream().filter(item -> item.sourceId().equals("noaa-nwr")).count());
    }

    private static FrequencyChannel channel(String id, GeoPoint point) {
        return new FrequencyChannel("test", id, "Test " + id, 155_000_000, DemodulationMode.NFM,
                "Test", "US", point, 1, FrequencyChannel.Confidence.LICENSED, Instant.now());
    }
}
