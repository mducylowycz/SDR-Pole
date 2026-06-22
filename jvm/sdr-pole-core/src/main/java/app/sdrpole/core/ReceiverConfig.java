package app.sdrpole.core;

public record ReceiverConfig(
        SdrDevice device,
        long frequencyHz,
        int sampleRate,
        double gainDb,
        boolean automaticGain,
        int audioSampleRate,
        DemodulationMode mode,
        RfFrontendSettings frontend,
        String audioOutput) {
    public ReceiverConfig {
        if (frequencyHz <= 0) throw new IllegalArgumentException("Frequency must be positive");
        if (sampleRate < 192_000) throw new IllegalArgumentException("Sample rate is too low");
        if (audioSampleRate < 8_000) throw new IllegalArgumentException("Audio sample rate is too low");
        if (mode == null) mode = DemodulationMode.NFM;
        if (frontend == null) frontend = RfFrontendSettings.safeDefaults(device);
        frontend = RfSafetyPolicy.sanitize(device, frequencyHz, frontend).settings();
        if (audioOutput == null) audioOutput = AudioOutputService.SYSTEM_DEFAULT;
    }

    public ReceiverConfig(SdrDevice device, long frequencyHz, int sampleRate, double gainDb,
                          boolean automaticGain, int audioSampleRate, DemodulationMode mode,
                          RfFrontendSettings frontend) {
        this(device, frequencyHz, sampleRate, gainDb, automaticGain, audioSampleRate, mode, frontend,
                AudioOutputService.SYSTEM_DEFAULT);
    }

    public ReceiverConfig(SdrDevice device, long frequencyHz, int sampleRate, double gainDb,
                          boolean automaticGain, int audioSampleRate) {
        this(device, frequencyHz, sampleRate, gainDb, automaticGain, audioSampleRate,
                DemodulationMode.NFM, RfFrontendSettings.safeDefaults(device), AudioOutputService.SYSTEM_DEFAULT);
    }

    public ReceiverConfig(SdrDevice device, long frequencyHz, int sampleRate, double gainDb,
                          boolean automaticGain, int audioSampleRate, DemodulationMode mode) {
        this(device, frequencyHz, sampleRate, gainDb, automaticGain, audioSampleRate,
                mode, RfFrontendSettings.safeDefaults(device), AudioOutputService.SYSTEM_DEFAULT);
    }
}
