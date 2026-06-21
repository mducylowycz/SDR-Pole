package app.sdrpole.core;

import java.util.List;

/** Offline US/North-American starter guide; named ranges replace blank-frequency guessing. */
public final class FrequencyBandCatalog {
    private FrequencyBandCatalog() {}
    public static List<FrequencyBand> northAmerica() {
        return List.of(
                new FrequencyBand("AM broadcast", 530_000, 1_700_000, 10_000, DemodulationMode.AM, "Local news, talk and music", "North America"),
                new FrequencyBand("10 m amateur", 28_000_000, 29_700_000, 5_000, DemodulationMode.USB, "Amateur voice, CW and data", "International"),
                new FrequencyBand("6 m amateur", 50_000_000, 54_000_000, 5_000, DemodulationMode.USB, "Amateur voice and data", "International"),
                new FrequencyBand("FM broadcast", 88_100_000, 107_900_000, 200_000, DemodulationMode.WFM, "Local music, news and public radio", "North America"),
                new FrequencyBand("Civil airband", 118_000_000, 136_975_000, 8_330, DemodulationMode.AM, "Aircraft and airport communications", "International"),
                new FrequencyBand("2 m amateur", 144_000_000, 148_000_000, 5_000, DemodulationMode.NFM, "Amateur repeaters, simplex and data", "North America"),
                new FrequencyBand("Marine VHF", 156_000_000, 162_025_000, 25_000, DemodulationMode.NFM, "Marine safety, calling and port operations", "International"),
                new FrequencyBand("NOAA Weather", 162_400_000, 162_550_000, 25_000, DemodulationMode.NFM, "Continuous weather broadcasts", "United States"),
                new FrequencyBand("Federal/public-safety VHF", 162_000_000, 174_000_000, 12_500, DemodulationMode.NFM, "Government and public-safety assignments; local records required", "United States"),
                new FrequencyBand("70 cm amateur", 420_000_000, 450_000_000, 12_500, DemodulationMode.NFM, "Amateur repeaters, simplex and digital", "United States"),
                new FrequencyBand("FRS / GMRS", 462_550_000, 467_725_000, 12_500, DemodulationMode.NFM, "Short-range personal and family radio", "United States"),
                new FrequencyBand("Public-safety 700 MHz", 769_000_000, 775_000_000, 12_500, DemodulationMode.NFM, "Public-safety systems; often P25 trunked", "United States"),
                new FrequencyBand("Public-safety 800 MHz", 851_000_000, 869_000_000, 12_500, DemodulationMode.NFM, "Public-safety and commercial trunked systems", "United States"),
                new FrequencyBand("ISM 915 MHz", 902_000_000, 928_000_000, 25_000, DemodulationMode.NFM, "Sensors, telemetry and low-power devices", "Americas"));
    }
}
