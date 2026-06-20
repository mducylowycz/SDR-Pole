package app.sdrpole.core;

import java.util.Set;

/** Assigns one independent tuner to one or more systems within its usable bandwidth. */
public record RadioAssignment(
        String deviceId,
        long centerFrequencyHz,
        int sampleRate,
        Set<String> systemIds,
        boolean automaticGain) {
    public RadioAssignment { systemIds = Set.copyOf(systemIds); }
}
