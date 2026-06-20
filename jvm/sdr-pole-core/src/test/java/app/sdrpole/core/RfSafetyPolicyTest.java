package app.sdrpole.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RfSafetyPolicyTest {
    private final SdrDevice hackrf = new SdrDevice("hackrf:1", "hackrf", "HackRF One", "1", true, Map.of());

    @Test void clampsHackRfGainsToVendorSteps() {
        var result = RfSafetyPolicy.sanitize(hackrf, 851_000_000,
                new RfFrontendSettings(39, 63, false, false, false));
        assertEquals(40, result.settings().lnaGainDb());
        assertEquals(62, result.settings().vgaGainDb());
    }

    @Test void blocksHighRiskControlsWithoutConfirmation() {
        var result = RfSafetyPolicy.sanitize(hackrf, 155_000_000,
                new RfFrontendSettings(16, 16, true, true, false));
        assertFalse(result.settings().rfAmplifier());
        assertFalse(result.settings().antennaPower());
        assertEquals(2, result.warnings().size());
    }

    @Test void rejectsOutOfRangeFrequency() {
        assertThrows(IllegalArgumentException.class, () -> RfSafetyPolicy.sanitize(hackrf, 500_000,
                RfFrontendSettings.safeDefaults(hackrf)));
    }
}
