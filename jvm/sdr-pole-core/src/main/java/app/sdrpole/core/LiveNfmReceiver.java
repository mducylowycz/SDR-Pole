package app.sdrpole.core;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Live receive-only SoapySDR stream with center-channel analog demodulation. */
public final class LiveNfmReceiver implements AutoCloseable {
    private static final int RX = 1;
    private final ReceiverConfig config;
    private final ReceiverListener listener;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile Pointer device;
    private volatile Pointer stream;
    private volatile Thread worker;

    public LiveNfmReceiver(ReceiverConfig config, ReceiverListener listener) {
        this.config = Objects.requireNonNull(config);
        this.listener = Objects.requireNonNull(listener);
    }

    public synchronized void start() {
        if (!running.compareAndSet(false, true)) return;
        worker = Thread.ofPlatform().name("sdr-pole-rx-" + config.device().id()).start(this::receive);
    }

    public boolean isRunning() { return running.get(); }

    public synchronized void tune(long frequencyHz) {
        if (device == null) return;
        check(SoapyNative.INSTANCE.SoapySDRDevice_setFrequency(device, RX, 0, frequencyHz, null), "Could not retune radio");
    }

    private void receive() {
        SourceDataLine audio = null;
        try {
            var nativeApi = SoapyNative.INSTANCE;
            var args = deviceArgs(config.device());
            device = nativeApi.SoapySDRDevice_makeStrArgs(args);
            if (device == null) throw new IllegalStateException("Radio could not be opened: " + nativeApi.SoapySDRDevice_lastError());
            check(nativeApi.SoapySDRDevice_setSampleRate(device, RX, 0, config.sampleRate()), "Unsupported sample rate");
            check(nativeApi.SoapySDRDevice_setFrequency(device, RX, 0, config.frequencyHz(), null), "Frequency is outside this radio's range");
            applyProtectedFrontend(nativeApi);

            stream = nativeApi.SoapySDRDevice_setupStream(device, RX, "CF32", null, 0, null);
            if (stream == null) throw new IllegalStateException("IQ stream could not start: " + nativeApi.SoapySDRDevice_lastError());
            check(nativeApi.SoapySDRDevice_activateStream(device, stream, 0, 0, 0), "Radio stream activation failed");

            var audioFormat = new AudioFormat(config.audioSampleRate(), 16, 1, true, false);
            audio = AudioSystem.getSourceDataLine(audioFormat);
            audio.open(audioFormat, config.audioSampleRate());
            audio.start();

            int elements = (int) Math.max(4096, Math.min(32768, nativeApi.SoapySDRDevice_getStreamMTU(device, stream)));
            var memory = new Memory((long) elements * 8);
            var buffers = new Pointer[]{memory};
            var flags = new IntByReference();
            var time = new LongByReference();
            var demod = new AnalogDemodulator(config.sampleRate(), config.audioSampleRate(), config.mode());
            int spectrumCountdown = 0;
            listener.onStatus(config.mode() + " at " + formatFrequency(config.frequencyHz()));

            while (running.get()) {
                int read = nativeApi.SoapySDRDevice_readStream(device, stream, buffers, elements, flags, time, 250_000);
                if (read > 0) {
                    var iq = memory.getFloatArray(0, read * 2);
                    var pcm = demod.accept(iq, read);
                    if (pcm.length > 0) audio.write(pcm, 0, pcm.length);
                    if (++spectrumCountdown >= 6) {
                        spectrumCountdown = 0;
                        listener.onSpectrum(Fft.powerDb(iq, read));
                        listener.onLevel(demod.lastRms());
                    }
                } else if (read < -1 && read != -4) {
                    throw new IllegalStateException("Radio stream error " + read + ": " + nativeApi.SoapySDRDevice_lastError());
                }
            }
        } catch (Throwable error) {
            if (running.get()) listener.onError(friendly(error), error);
        } finally {
            running.set(false);
            if (audio != null) { audio.stop(); audio.close(); }
            cleanup();
            listener.onStatus("Stopped");
        }
    }

    private void cleanup() {
        var api = SoapyNative.INSTANCE;
        var localDevice = device;
        var localStream = stream;
        stream = null;
        device = null;
        if (localDevice != null && localStream != null) {
            api.SoapySDRDevice_deactivateStream(localDevice, localStream, 0, 0);
            api.SoapySDRDevice_closeStream(localDevice, localStream);
        }
        if (localDevice != null) api.SoapySDRDevice_unmake(localDevice);
    }

    private void applyProtectedFrontend(SoapyNative nativeApi) {
        var safe = RfSafetyPolicy.sanitize(config.device(), config.frequencyHz(), config.frontend()).settings();
        nativeApi.SoapySDRDevice_setGainMode(device, RX, 0, false);
        if ("hackrf".equalsIgnoreCase(config.device().driver())) {
            // Power and near-antenna RF amplification are explicitly driven to a known state on every open.
            nativeApi.SoapySDRDevice_writeSetting(device, "bias_tx", Boolean.toString(safe.antennaPower()));
            check(nativeApi.SoapySDRDevice_setGainElement(device, RX, 0, "AMP", safe.rfAmplifier() ? 11 : 0), "Could not set protected RF amplifier state");
            check(nativeApi.SoapySDRDevice_setGainElement(device, RX, 0, "LNA", safe.lnaGainDb()), "Unsupported LNA gain");
            check(nativeApi.SoapySDRDevice_setGainElement(device, RX, 0, "VGA", safe.vgaGainDb()), "Unsupported VGA gain");
        } else if (!config.automaticGain()) {
            check(nativeApi.SoapySDRDevice_setGain(device, RX, 0, config.gainDb()), "Unsupported gain");
        }
    }

    @Override public synchronized void close() {
        running.set(false);
        var thread = worker;
        if (thread != null) {
            try { thread.join(1500); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    private static String deviceArgs(SdrDevice device) {
        var args = new StringBuilder("driver=").append(device.driver());
        if (device.serial() != null && !device.serial().isBlank()) args.append(",serial=").append(device.serial());
        return args.toString();
    }

    private static void check(int status, String message) {
        if (status != 0) throw new IllegalStateException(message + ": " + SoapyNative.INSTANCE.SoapySDRDevice_lastError());
    }

    private static String friendly(Throwable error) {
        var message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        if (message.contains("busy") || message.contains("claim")) return "This radio is open in another application. Close the other SDR app and retry.";
        if (message.contains("Audio") || message.contains("line")) return "Audio output could not start. Choose another output device in Settings.";
        if (message.contains("SoapySDR")) return "The SDR driver is missing. Open Devices and install the recommended driver.";
        return message;
    }

    private static String formatFrequency(long hz) { return String.format("%.5f MHz", hz / 1_000_000.0); }

    private static final class AnalogDemodulator {
        private final double inputRate;
        private final double outputRate;
        private final DemodulationMode mode;
        private final double deemphasisAlpha;
        private double outputPhase;
        private double bfoPhase;
        private float previousI = 1;
        private float previousQ;
        private double filtered;
        private double dc;
        private double rms;

        AnalogDemodulator(double inputRate, double outputRate, DemodulationMode mode) {
            this.inputRate = inputRate;
            this.outputRate = outputRate;
            this.mode = mode;
            this.deemphasisAlpha = Math.exp(-1.0 / (inputRate * (mode == DemodulationMode.WFM ? 75e-6 : 50e-6)));
        }

        byte[] accept(float[] iq, int count) {
            int expected = Math.max(1, (int) Math.ceil(count * outputRate / inputRate));
            var out = ByteBuffer.allocate(expected * 2 + 4).order(ByteOrder.LITTLE_ENDIAN);
            double sumSquares = 0;
            int produced = 0;
            for (int n = 0; n < count; n++) {
                float i = iq[n * 2], q = iq[n * 2 + 1];
                double sample;
                if (mode == DemodulationMode.NFM || mode == DemodulationMode.WFM) {
                    double discriminator = Math.atan2(previousI * q - previousQ * i, previousI * i + previousQ * q);
                    double deviation = mode == DemodulationMode.WFM ? 75_000 : 5_000;
                    sample = discriminator * inputRate / (2 * Math.PI * deviation);
                    filtered = deemphasisAlpha * filtered + (1 - deemphasisAlpha) * sample;
                    sample = filtered;
                } else if (mode == DemodulationMode.AM) {
                    var envelope = Math.hypot(i, q);
                    dc = dc * 0.9999 + envelope * 0.0001;
                    sample = (envelope - dc) * 3.0;
                } else {
                    double tone = mode == DemodulationMode.CW ? 700 : 1_500;
                    double direction = mode == DemodulationMode.LSB ? -1 : 1;
                    sample = i * Math.cos(bfoPhase) - q * Math.sin(bfoPhase) * direction;
                    bfoPhase += direction * 2 * Math.PI * tone / inputRate;
                    if (bfoPhase > Math.PI) bfoPhase -= 2 * Math.PI;
                    if (bfoPhase < -Math.PI) bfoPhase += 2 * Math.PI;
                }
                previousI = i; previousQ = q;
                outputPhase += outputRate;
                if (outputPhase >= inputRate) {
                    outputPhase -= inputRate;
                    sample = Math.max(-1, Math.min(1, sample));
                    out.putShort((short) (sample * 32767));
                    sumSquares += sample * sample;
                    produced++;
                }
            }
            rms = produced == 0 ? 0 : Math.sqrt(sumSquares / produced);
            var bytes = new byte[out.position()];
            out.flip(); out.get(bytes);
            return bytes;
        }

        double lastRms() { return rms; }
    }
}
