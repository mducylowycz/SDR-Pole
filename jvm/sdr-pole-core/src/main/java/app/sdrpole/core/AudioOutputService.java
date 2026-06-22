package app.sdrpole.core;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.List;

/** Enumerates, opens, and safely tests local Java Sound outputs. */
public final class AudioOutputService {
    public static final String SYSTEM_DEFAULT = "";
    private static final AudioFormat FORMAT = new AudioFormat(48_000, 16, 1, true, false);

    public record Output(String id, String label) {
        @Override public String toString() { return label; }
    }

    public List<Output> outputs() {
        var result = new ArrayList<Output>();
        if (AudioSystem.isLineSupported(info(FORMAT))) result.add(new Output(SYSTEM_DEFAULT, "System default"));
        for (var mixerInfo : AudioSystem.getMixerInfo()) {
            try {
                var mixer = AudioSystem.getMixer(mixerInfo);
                if (mixer.isLineSupported(info(FORMAT))) result.add(new Output(mixerInfo.getName(), mixerInfo.getName()));
            } catch (RuntimeException ignored) { /* A disconnected mixer must not break setup. */ }
        }
        return List.copyOf(result);
    }

    public SourceDataLine open(String outputId, AudioFormat format, int bufferBytes) throws Exception {
        SourceDataLine line;
        if (outputId == null || outputId.isBlank()) line = AudioSystem.getSourceDataLine(format);
        else {
            var mixer = findMixer(outputId);
            line = (SourceDataLine) mixer.getLine(info(format));
        }
        line.open(format, bufferBytes);
        return line;
    }

    public void playTestTone(String outputId) throws Exception {
        var line = open(outputId, FORMAT, 9_600);
        try {
            line.start();
            var pcm = new byte[24_000];
            for (int frame = 0; frame < pcm.length / 2; frame++) {
                short sample = (short) (Math.sin(2 * Math.PI * 523.25 * frame / FORMAT.getSampleRate()) * 5_500);
                pcm[frame * 2] = (byte) sample; pcm[frame * 2 + 1] = (byte) (sample >>> 8);
            }
            line.write(pcm, 0, pcm.length); line.drain();
        } finally { line.stop(); line.close(); }
    }

    public boolean available() { return !outputs().isEmpty(); }

    private static DataLine.Info info(AudioFormat format) { return new DataLine.Info(SourceDataLine.class, format); }
    private static Mixer findMixer(String id) {
        for (var info : AudioSystem.getMixerInfo()) if (info.getName().equals(id)) return AudioSystem.getMixer(info);
        throw new IllegalArgumentException("Audio output is no longer connected: " + id);
    }
}
