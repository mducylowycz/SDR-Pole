package app.sdrpole.core.p25;

import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.SdrDevice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class P25RuntimeConfiguratorTest {
    @TempDir Path temp;

    @Test void choosesNearestSiteAndCreatesSafeHackRfPlan() throws Exception {
        var hackrf = new SdrDevice("hackrf:1", "hackrf", "HackRF One", "abc123", true, Map.of());
        var far = site("Statewide", "Far", 851_100_000L, new GeoPoint(35, -90), false);
        var near = site("County", "Simulcast", 852_125_000L, new GeoPoint(41, -87), true);
        var plan = new P25RuntimeConfigurator(temp).create(List.of(hackrf), List.of(far, near), new GeoPoint(41.01, -87.01),
                List.of(new P25Talkgroup("County", 101, "Fire Dispatch", "County Fire", "D", 0),
                        new P25Talkgroup("County", 202, "Police Tac", "Encrypted", "DE", 2)));

        assertEquals(near, plan.system());
        assertEquals(8_000_000, plan.sampleRate());
        assertEquals(4, plan.voiceTaps());
        assertEquals(2, plan.talkgroupCount());
        var yaml = Files.readString(plan.configFile());
        assertTrue(yaml.contains("http_addr: \"127.0.0.1:18080\""));
        assertTrue(yaml.contains("p25_phase1_demod_mode: \"cqpsk\""));
        assertTrue(yaml.contains("autotune: true"));
        assertTrue(yaml.contains("skip_encrypted: true"));
        assertTrue(yaml.contains("talkgroup_file:"));
        assertFalse(yaml.contains("latitude"));
        var aliases = Files.readString(temp.resolve("config/talkgroups-county.csv"));
        assertTrue(aliases.contains("Fire Dispatch"));
        assertTrue(aliases.contains("202,\"Police Tac\",\"Encrypted\",\"DE\",\"County\",0,true,false"));
    }

    @Test void refusesToGuessWhenRequiredInputsAreMissing() {
        var configurator = new P25RuntimeConfigurator(temp);
        assertThrows(IllegalStateException.class, () -> configurator.create(List.of(), List.of(), null));
    }

    private static P25SystemConfig site(String system, String site, long frequency, GeoPoint point, boolean simulcast) {
        return new P25SystemConfig(system, site, List.of(frequency), simulcast
                ? P25SystemConfig.Modulation.LSM_SIMULCAST : P25SystemConfig.Modulation.C4FM, 4, point);
    }
}
