package app.sdrpole.core;

import java.util.Optional;

/** A frequency found from public records, an authenticated directory, an import, or live RF energy. */
public record FrequencyCandidate(
        long frequencyHz,
        String label,
        String service,
        Source source,
        GeoPoint location,
        Double signalDb,
        double confidence) {
    public FrequencyCandidate {
        if (frequencyHz <= 0) throw new IllegalArgumentException("Frequency must be positive");
        if (confidence < 0 || confidence > 1) throw new IllegalArgumentException("Confidence must be from 0 to 1");
    }

    public Optional<GeoPoint> knownLocation() { return Optional.ofNullable(location); }
    public Optional<Double> measuredSignalDb() { return Optional.ofNullable(signalDb); }

    public enum Source { FCC_ULS, RADIO_REFERENCE, LIVE_SCAN, IMPORTED, USER }
}
