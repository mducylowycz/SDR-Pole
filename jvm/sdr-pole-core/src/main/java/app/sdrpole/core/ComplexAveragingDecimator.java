package app.sdrpole.core;

/** Stateful integrate-and-dump complex decimator that suppresses wideband aliases before demodulation. */
final class ComplexAveragingDecimator {
    record Samples(float[] iq, int count, double sampleRate) {}

    private final int factor;
    private final double outputRate;
    private int accumulated;
    private double sumI;
    private double sumQ;

    ComplexAveragingDecimator(double inputRate, double targetRate) {
        factor = Math.max(1, (int) Math.floor(inputRate / targetRate));
        outputRate = inputRate / factor;
    }

    Samples accept(float[] iq, int count) {
        var output = new float[(count / factor + 2) * 2];
        int produced = 0;
        for (int index = 0; index < count; index++) {
            sumI += iq[index * 2]; sumQ += iq[index * 2 + 1]; accumulated++;
            if (accumulated == factor) {
                output[produced * 2] = (float) (sumI / factor);
                output[produced * 2 + 1] = (float) (sumQ / factor);
                produced++; accumulated = 0; sumI = 0; sumQ = 0;
            }
        }
        return new Samples(output, produced, outputRate);
    }
}
