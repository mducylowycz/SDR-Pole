package app.sdrpole.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Center-channel analog demodulator with complex anti-alias decimation and audio conditioning. */
final class AnalogAudioDemodulator {
    private final ComplexAveragingDecimator channelizer;
    private final double outputRate;
    private final DemodulationMode mode;
    private double channelRate;
    private double outputPhase;
    private double bfoPhase;
    private float previousI = 1;
    private float previousQ;
    private double deemphasis;
    private double audioLowPass;
    private double dc;
    private double levelEnvelope = 0.1;
    private double rms;

    AnalogAudioDemodulator(double inputRate, double outputRate, DemodulationMode mode) {
        this.outputRate = outputRate;
        this.mode = mode;
        channelizer = new ComplexAveragingDecimator(inputRate, mode == DemodulationMode.WFM ? 384_000 :
                (mode == DemodulationMode.NFM ? 192_000 : 96_000));
    }

    byte[] accept(float[] wideIq, int count) {
        var channel = channelizer.accept(wideIq, count);
        channelRate = channel.sampleRate();
        int expected = Math.max(1, (int) Math.ceil(channel.count() * outputRate / channelRate));
        var out = ByteBuffer.allocate(expected * 2 + 4).order(ByteOrder.LITTLE_ENDIAN);
        double sumSquares = 0;
        int produced = 0;
        for (int n = 0; n < channel.count(); n++) {
            float i = channel.iq()[n * 2], q = channel.iq()[n * 2 + 1];
            double sample = demodulate(i, q);
            double cutoff = switch (mode) {
                case WFM -> 15_000; case NFM, AM -> 4_500; case USB, LSB, CW -> 3_000;
            };
            double alpha = 1 - Math.exp(-2 * Math.PI * cutoff / channelRate);
            audioLowPass += alpha * (sample - audioLowPass);
            outputPhase += outputRate;
            if (outputPhase >= channelRate) {
                outputPhase -= channelRate;
                double conditioned = condition(audioLowPass);
                out.putShort((short) (conditioned * 32767));
                sumSquares += conditioned * conditioned; produced++;
            }
        }
        rms = produced == 0 ? 0 : Math.sqrt(sumSquares / produced);
        var bytes = new byte[out.position()]; out.flip(); out.get(bytes); return bytes;
    }

    private double demodulate(float i, float q) {
        double sample;
        if (mode == DemodulationMode.NFM || mode == DemodulationMode.WFM) {
            double discriminator = Math.atan2(previousI * q - previousQ * i, previousI * i + previousQ * q);
            double deviation = mode == DemodulationMode.WFM ? 75_000 : 5_000;
            sample = discriminator * channelRate / (2 * Math.PI * deviation);
            double alpha = Math.exp(-1.0 / (channelRate * (mode == DemodulationMode.WFM ? 75e-6 : 50e-6)));
            deemphasis = alpha * deemphasis + (1 - alpha) * sample; sample = deemphasis;
        } else if (mode == DemodulationMode.AM) {
            double envelope = Math.hypot(i, q); dc = dc * 0.999 + envelope * 0.001; sample = (envelope - dc) * 3;
        } else {
            double tone = mode == DemodulationMode.CW ? 700 : 1_500;
            double direction = mode == DemodulationMode.LSB ? -1 : 1;
            sample = i * Math.cos(bfoPhase) - q * Math.sin(bfoPhase) * direction;
            bfoPhase += direction * 2 * Math.PI * tone / channelRate;
            if (bfoPhase > Math.PI) bfoPhase -= 2 * Math.PI;
            if (bfoPhase < -Math.PI) bfoPhase += 2 * Math.PI;
        }
        previousI = i; previousQ = q; return sample;
    }

    private double condition(double sample) {
        levelEnvelope = Math.max(Math.abs(sample), levelEnvelope * 0.9995);
        double gain = Math.max(0.3, Math.min(5, 0.35 / Math.max(0.01, levelEnvelope)));
        return Math.tanh(sample * gain);
    }

    double lastRms() { return rms; }
}
