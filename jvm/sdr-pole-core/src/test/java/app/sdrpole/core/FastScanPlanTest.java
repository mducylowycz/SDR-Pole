package app.sdrpole.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FastScanPlanTest {
    @Test void coversWideRangeWithFftWindowsInsteadOfEveryChannel() {
        var plan = new FastScanPlan(List.of(new ScanRange(851_000_000, 869_000_000, 12_500, DemodulationMode.NFM)), 2_000_000);
        assertTrue(plan.windowCount() >= 12 && plan.windowCount() <= 16);
        assertTrue(plan.windowCount() < 18_000_000 / 12_500);
        assertEquals(1, plan.next().rangeNumber());
    }

    @Test void centersNarrowRangeOnce() {
        var plan = new FastScanPlan(List.of(new ScanRange(162_400_000, 162_550_000, 25_000, DemodulationMode.NFM)), 2_000_000);
        assertEquals(1, plan.windowCount());
        assertEquals(162_475_000, plan.next().centerFrequencyHz());
    }
}
