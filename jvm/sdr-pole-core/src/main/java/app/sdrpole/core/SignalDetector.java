package app.sdrpole.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Robust noise-floor estimation and contiguous peak grouping for live FFT frames. */
public final class SignalDetector {
    public List<SignalObservation> detect(float[] powerDb, long centerHz, int sampleRate, double thresholdAboveNoiseDb) {
        if (powerDb == null || powerDb.length < 16) return List.of();
        var sorted = powerDb.clone();
        java.util.Arrays.sort(sorted);
        double noise = sorted[(int) (sorted.length * .35)];
        double threshold = noise + Math.max(6, thresholdAboveNoiseDb);
        double binHz = sampleRate / (double) powerDb.length;
        var found = new ArrayList<SignalObservation>();
        int i = 0;
        while (i < powerDb.length) {
            if (powerDb[i] < threshold) { i++; continue; }
            int start = i, peakBin = i;
            float peak = powerDb[i];
            while (i + 1 < powerDb.length && powerDb[i + 1] >= threshold - 2) {
                i++;
                if (powerDb[i] > peak) { peak = powerDb[i]; peakBin = i; }
            }
            int end = i++;
            if (end - start < 1) continue;
            double offset = (peakBin - powerDb.length / 2.0) * binHz;
            if (Math.abs(offset) < binHz * 2) continue; // common zero-IF DC spike
            double bandwidth = Math.max(binHz * 2, (end - start + 1) * binHz);
            found.add(new SignalObservation(Math.round(centerHz + offset), peak, noise, bandwidth,
                    DecoderHint.fromBandwidth(bandwidth)));
        }
        found.sort(Comparator.comparingDouble(SignalObservation::snrDb).reversed());
        return List.copyOf(found);
    }
}
