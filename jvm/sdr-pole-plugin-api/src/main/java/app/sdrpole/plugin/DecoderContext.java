package app.sdrpole.plugin;

import java.util.function.Consumer;

public record DecoderContext(
        int iqSampleRate,
        int audioSampleRate,
        Consumer<float[]> audioSink,
        Consumer<DecoderEvent> eventSink) {}
