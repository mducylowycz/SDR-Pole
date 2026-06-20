package app.sdrpole.core.p25;

import app.sdrpole.core.GeoPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class P25SystemStoreTest {
    @Test void atomicallyRoundTripsSiteConfiguration(@TempDir Path directory) throws Exception {
        var store = new P25SystemStore(directory.resolve("systems.json"));
        var config = new P25SystemConfig("County", "North", List.of(851_112_500L),
                P25SystemConfig.Modulation.LSM_SIMULCAST, 3, new GeoPoint(41.1, -87.2));
        store.save(List.of(config));
        assertEquals(List.of(config), store.load());
    }
}
