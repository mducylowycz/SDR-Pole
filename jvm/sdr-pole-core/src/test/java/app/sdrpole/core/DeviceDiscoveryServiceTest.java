package app.sdrpole.core;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DeviceDiscoveryServiceTest {
    @Test void parsesMultipleSoapyDevices() {
        var devices = DeviceDiscoveryService.parseSoapyFind("""
                Found device 0
                  driver = hackrf
                  label = HackRF One
                  serial = ABC123
                Found device 1
                  driver = rtlsdr
                  label = Generic RTL2832U
                  serial = 00000001
                """);
        assertEquals(2, devices.size());
        assertEquals("hackrf:ABC123", devices.getFirst().id());
        assertEquals("rtlsdr", devices.get(1).driver());
    }
}
