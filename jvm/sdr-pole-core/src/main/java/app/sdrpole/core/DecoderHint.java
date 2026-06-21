package app.sdrpole.core;

/** Honest, spectrum-only decoder suggestion. Frame sync is required for protocol confirmation. */
public record DecoderHint(String label, DemodulationMode mode, double confidence, boolean needsFrameConfirmation) {
    public static DecoderHint fromBandwidth(double bandwidthHz) {
        if (bandwidthHz < 3_500) return new DecoderHint("CW / narrow data", DemodulationMode.CW, .58, true);
        if (bandwidthHz < 9_000) return new DecoderHint("Narrow FM", DemodulationMode.NFM, .62, false);
        if (bandwidthHz < 18_000) return new DecoderHint("NFM or P25 candidate", DemodulationMode.NFM, .55, true);
        if (bandwidthHz < 45_000) return new DecoderHint("Digital or narrow voice", DemodulationMode.NFM, .42, true);
        if (bandwidthHz > 120_000) return new DecoderHint("Wide FM", DemodulationMode.WFM, .70, false);
        return new DecoderHint("AM / unknown", DemodulationMode.AM, .35, true);
    }
}
