package app.sdrpole.core;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ScanPlanTest {
    @Test void cyclesAcrossEverySelectedRangeAndRoundTrips() {
        var ranges = List.of(new ScanRange(100, 120, 10, DemodulationMode.AM), new ScanRange(200, 200, 5, DemodulationMode.NFM));
        var decoded = ScanPlan.decode(ScanPlan.encode(ranges));
        assertEquals(ranges, decoded);
        var plan = new ScanPlan(decoded);
        assertEquals(100, plan.next().frequencyHz());
        assertEquals(110, plan.next().frequencyHz());
        assertEquals(120, plan.next().frequencyHz());
        assertEquals(200, plan.next().frequencyHz());
        assertEquals(100, plan.next().frequencyHz());
    }
}
