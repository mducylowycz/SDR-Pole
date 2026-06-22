package app.sdrpole.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnalogAudioDemodulatorTest {
    @Test void antiAliasDecimatorRejectsAnAlternatingWidebandComponent() {
        var decimator = new ComplexAveragingDecimator(800_000, 200_000);
        var iq = new float[800];
        for (int sample = 0; sample < iq.length / 2; sample++) iq[sample * 2] = sample % 2 == 0 ? 1 : -1;
        var result = decimator.accept(iq, iq.length / 2);
        assertEquals(100, result.count());
        for (int index = 0; index < result.count() * 2; index++) assertEquals(0, result.iq()[index], 1e-6);
    }

    @Test void narrowFmProducesConditionedAudioFromEightMegasampleInput() {
        int rate = 8_000_000;
        var iq = new float[rate / 100 * 2];
        double phase = 0;
        for (int sample = 0; sample < iq.length / 2; sample++) {
            double modulation = Math.sin(2 * Math.PI * 1_000 * sample / rate);
            phase += 2 * Math.PI * 4_000 * modulation / rate;
            iq[sample * 2] = (float) Math.cos(phase); iq[sample * 2 + 1] = (float) Math.sin(phase);
        }
        var demodulator = new AnalogAudioDemodulator(rate, 48_000, DemodulationMode.NFM);
        var pcm = demodulator.accept(iq, iq.length / 2);
        assertTrue(pcm.length >= 900 && pcm.length <= 1_000);
        assertTrue(demodulator.lastRms() > 0.05);
    }
}
