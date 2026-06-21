package app.sdrpole.core;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SignalDetectorTest {
    @Test void findsStrongOffCenterSignalAndSuggestsDecoder() {
        var bins = new float[1024];
        java.util.Arrays.fill(bins, -105);
        for (int i = 620; i < 628; i++) bins[i] = -62;
        var result = new SignalDetector().detect(bins, 100_000_000, 2_000_000, 12);
        assertEquals(1, result.size());
        assertTrue(result.getFirst().frequencyHz() > 100_200_000);
        assertTrue(result.getFirst().snrDb() > 35);
    }

    @Test void autoTuneRequiresStableConfirmations() {
        var observation = new SignalObservation(155_250_000, -60, -100, 12_500,
                DecoderHint.fromBandwidth(12_500));
        var policy = new AutoTunePolicy(12, 3);
        var now = Instant.now();
        assertTrue(policy.consider(java.util.List.of(observation), now).isEmpty());
        assertTrue(policy.consider(java.util.List.of(observation), now.plusMillis(100)).isEmpty());
        assertTrue(policy.consider(java.util.List.of(observation), now.plusMillis(200)).isPresent());
    }
}
