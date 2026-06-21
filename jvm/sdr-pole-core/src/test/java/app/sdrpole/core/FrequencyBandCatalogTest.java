package app.sdrpole.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FrequencyBandCatalogTest {
    @Test void bundledGuideHasNamedValidRangesWithoutBlankGuessing() {
        var bands = FrequencyBandCatalog.northAmerica();
        assertTrue(bands.size() >= 12);
        assertTrue(bands.stream().allMatch(b -> !b.name().isBlank() && !b.commonUse().isBlank() && b.stepHz() > 0));
        assertTrue(bands.stream().anyMatch(b -> b.name().contains("airband") && b.mode() == DemodulationMode.AM));
    }
}
