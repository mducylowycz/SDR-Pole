package app.sdrpole.core;

public record ScanRange(long startHz, long endHz, long stepHz, DemodulationMode mode) {
    public ScanRange {
        if (startHz <= 0 || endHz < startHz || stepHz <= 0 || mode == null) throw new IllegalArgumentException("Invalid scan range");
    }
}
