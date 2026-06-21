package app.sdrpole.core.p25;

import app.sdrpole.core.SdrDevice;

import java.nio.file.Path;
import java.util.List;

/** A complete, user-reviewable runtime plan produced without exposing engine configuration. */
public record P25RuntimePlan(P25SystemConfig system, SdrDevice controlDevice,
                             List<SdrDevice> voiceDevices, long centerFrequencyHz,
                             int sampleRate, int voiceTaps, Path configFile,
                             List<String> notes) {
    public P25RuntimePlan {
        voiceDevices = List.copyOf(voiceDevices);
        notes = List.copyOf(notes);
    }
}
