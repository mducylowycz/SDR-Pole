package app.sdrpole.core.hackrf;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HackRfProfileServiceTest {
    @Test void parsesOneAndProProfilesAndFlagsVersionMismatch() {
        var output = """
                hackrf_info version: 2026.01.3
                libhackrf version: 2026.01.3 (0.9.2)
                Found HackRF
                Index: 0
                Serial number: 00000001
                Board ID Number: 2 (HackRF One)
                Firmware Version: 2024.02.1 (API:1.08)
                Part ID Number: 0xa000 0xb000
                Found HackRF
                Index: 1
                Serial number: 00000002
                Board ID Number: 4 (HackRF Pro)
                Firmware Version: 2026.01.3 (API:1.09)
                Part ID Number: 0xc000 0xd000
                """;
        var profiles = HackRfProfileService.parse(output, true, true, true);
        assertEquals(2, profiles.size()); assertEquals("HackRF One", profiles.getFirst().board());
        assertFalse(profiles.getFirst().pro()); assertTrue(profiles.getFirst().findings().getFirst().contains("differ"));
        assertTrue(profiles.getLast().pro()); assertEquals(100_000, profiles.getLast().minimumFrequencyHz());
        assertEquals("1.09", profiles.getLast().firmwareApi());
    }
}
