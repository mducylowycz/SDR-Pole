package app.sdrpole.core.p25;

import app.sdrpole.core.GeoPoint;

import java.util.List;

public record P25SystemConfig(String systemName, String siteName, List<Long> controlFrequenciesHz,
                              Modulation modulation, int maxTrafficChannels, GeoPoint location) {
    public P25SystemConfig {
        if (systemName == null || systemName.isBlank()) throw new IllegalArgumentException("System name is required");
        if (siteName == null || siteName.isBlank()) throw new IllegalArgumentException("Site name is required");
        controlFrequenciesHz = List.copyOf(controlFrequenciesHz);
        if (controlFrequenciesHz.isEmpty() || controlFrequenciesHz.stream().anyMatch(value -> value <= 0))
            throw new IllegalArgumentException("At least one valid control-channel frequency is required");
        if (maxTrafficChannels < 1 || maxTrafficChannels > 32) throw new IllegalArgumentException("Traffic channel limit must be 1–32");
    }

    public enum Modulation { C4FM, LSM_SIMULCAST }
}
