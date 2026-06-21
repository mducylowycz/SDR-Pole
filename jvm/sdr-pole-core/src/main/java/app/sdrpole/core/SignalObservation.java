package app.sdrpole.core;

/** One energy peak measured in the current receiver passband. */
public record SignalObservation(long frequencyHz, double peakDb, double noiseFloorDb,
                                double bandwidthHz, DecoderHint hint) {
    public double snrDb() { return peakDb - noiseFloorDb; }
}
