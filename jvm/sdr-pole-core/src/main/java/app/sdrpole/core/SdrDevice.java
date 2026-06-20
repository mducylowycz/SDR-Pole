package app.sdrpole.core;

import java.util.Map;

/** A physical or network SDR discovered through a vendor-neutral provider. */
public record SdrDevice(
        String id,
        String driver,
        String label,
        String serial,
        boolean available,
        Map<String, String> properties) {
    public SdrDevice {
        properties = Map.copyOf(properties);
    }
}
