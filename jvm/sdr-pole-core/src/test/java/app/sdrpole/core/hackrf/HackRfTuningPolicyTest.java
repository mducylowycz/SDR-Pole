package app.sdrpole.core.hackrf;

import app.sdrpole.core.SdrDevice;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HackRfTuningPolicyTest {
    @Test void usesNativeHackRfRatesAndAnEightMegasampleDefault() {
        var device = new SdrDevice("hackrf:1", "hackrf", "HackRF One", "1", true, Map.of());
        assertEquals(8_000_000, HackRfTuningPolicy.recommendedReceiveSampleRate(device));
        assertEquals(8_000_000, HackRfTuningPolicy.receiveSampleRates(device).getFirst());
        assertEquals(20_000_000, HackRfTuningPolicy.receiveSampleRates(device).getLast());
        assertFalse(HackRfTuningPolicy.receiveSampleRates(device).contains(2_000_000));
    }

    @Test void preservesConservativeGenericDefaults() {
        var device = new SdrDevice("rtlsdr:1", "rtlsdr", "RTL-SDR", "1", true, Map.of());
        assertEquals(2_000_000, HackRfTuningPolicy.recommendedReceiveSampleRate(device));
        assertFalse(HackRfTuningPolicy.appliesTo(device));
    }
}
