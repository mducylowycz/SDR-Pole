package app.sdrpole.core.directory;

import app.sdrpole.core.DemodulationMode;
import app.sdrpole.core.GeoPoint;

import java.time.Instant;

/** One receive channel with explicit provenance; a license record is never treated as measured activity. */
public record FrequencyChannel(String sourceId, String externalId, String name, long frequencyHz,
                               DemodulationMode mode, String commonUse, String region,
                               GeoPoint location, double locationAccuracyKm, Confidence confidence,
                               Instant updatedAt) {
    public enum Confidence { REFERENCE, LICENSED, COMMUNITY, MEASURED }

    public FrequencyChannel {
        if (sourceId == null || sourceId.isBlank() || externalId == null || externalId.isBlank())
            throw new IllegalArgumentException("Frequency channel needs a source and stable identifier");
        if (name == null || name.isBlank() || frequencyHz <= 0 || mode == null || confidence == null || updatedAt == null)
            throw new IllegalArgumentException("Frequency channel is incomplete");
        commonUse = commonUse == null ? "" : commonUse;
        region = region == null ? "" : region;
        if (locationAccuracyKm < 0) throw new IllegalArgumentException("Location accuracy cannot be negative");
    }

    public String frequencyLabel() { return String.format("%.5f MHz", frequencyHz / 1e6); }
}
