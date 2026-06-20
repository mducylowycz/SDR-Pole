package app.sdrpole.core;

import java.util.List;

/** Beginner-friendly examples; users can always type any lawful receive frequency. */
public record FrequencyPreset(String name, double frequencyMhz, DemodulationMode mode, String region) {
    public static List<FrequencyPreset> commonNorthAmerica() {
        return List.of(
                new FrequencyPreset("NOAA Weather 1", 162.550, DemodulationMode.NFM, "US"),
                new FrequencyPreset("NOAA Weather 2", 162.400, DemodulationMode.NFM, "US"),
                new FrequencyPreset("NOAA Weather 3", 162.475, DemodulationMode.NFM, "US"),
                new FrequencyPreset("2 m calling", 146.520, DemodulationMode.NFM, "North America"),
                new FrequencyPreset("FM broadcast example", 99.500, DemodulationMode.WFM, "Example"),
                new FrequencyPreset("AM broadcast example", 1.000, DemodulationMode.AM, "Example"));
    }

    @Override public String toString() { return name + "  •  " + frequencyMhz + " MHz"; }
}
