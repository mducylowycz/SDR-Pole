package app.sdrpole.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HackRfSweepServiceTest {
    @Test void parsesRanksAndMergesHardwareSweepBins() throws Exception {
        var csv = "2026-01-01, 00:00:00, 851000000, 851400000, 100000, 4, -80, -79, -35, -78\n"
                + "2026-01-01, 00:00:00, 852000000, 852400000, 100000, 4, -81, -40, -80, -79\n";
        var peaks = HackRfSweepService.parse(csv, 12, 20);
        assertEquals(2, peaks.size());
        assertEquals(851_250_000, peaks.getFirst().frequencyHz());
        assertTrue(peaks.getFirst().powerDb() > peaks.getLast().powerDb());
    }
}
