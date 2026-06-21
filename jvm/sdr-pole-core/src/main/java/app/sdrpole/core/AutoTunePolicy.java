package app.sdrpole.core;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Debounces strong peaks so one noisy FFT frame cannot retune the radio. */
public final class AutoTunePolicy {
    private final double minimumSnrDb;
    private final int confirmations;
    private long candidateHz;
    private int seen;
    private Instant lastTune = Instant.EPOCH;

    public AutoTunePolicy(double minimumSnrDb, int confirmations) {
        this.minimumSnrDb = minimumSnrDb;
        this.confirmations = Math.max(1, confirmations);
    }

    public Optional<SignalObservation> consider(List<SignalObservation> observations, Instant now) {
        if (Duration.between(lastTune, now).compareTo(Duration.ofSeconds(3)) < 0 || observations.isEmpty()) return Optional.empty();
        var strongest = observations.getFirst();
        if (strongest.snrDb() < minimumSnrDb) { seen = 0; return Optional.empty(); }
        if (Math.abs(strongest.frequencyHz() - candidateHz) <= Math.max(2_500, strongest.bandwidthHz() / 2)) seen++;
        else { candidateHz = strongest.frequencyHz(); seen = 1; }
        if (seen < confirmations) return Optional.empty();
        seen = 0;
        lastTune = now;
        return Optional.of(strongest);
    }
}
