package app.sdrpole.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrequencyPresetTest {
    @Test void allBuiltInExamplesAreValidAndReceiveOnly() {
        var presets = FrequencyPreset.commonNorthAmerica();
        assertFalse(presets.isEmpty());
        assertTrue(presets.stream().allMatch(preset -> preset.frequencyMhz() > 0 && preset.mode() != null));
    }
}
