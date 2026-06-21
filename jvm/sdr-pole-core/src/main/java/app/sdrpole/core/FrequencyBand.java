package app.sdrpole.core;

/** A named receive range from the bundled frequency guide. */
public record FrequencyBand(String name, long startHz, long endHz, long stepHz,
                            DemodulationMode mode, String commonUse, String region) {
    public FrequencyBand {
        if (startHz <= 0 || endHz < startHz || stepHz <= 0) throw new IllegalArgumentException("Invalid frequency range");
    }
    public String rangeLabel() {
        return startHz == endHz ? String.format("%.5f MHz", startHz / 1e6)
                : String.format("%.3f–%.3f MHz", startHz / 1e6, endHz / 1e6);
    }
}
