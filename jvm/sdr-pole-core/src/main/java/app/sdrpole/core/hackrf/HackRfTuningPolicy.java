package app.sdrpole.core.hackrf;

import app.sdrpole.core.SdrDevice;

import java.util.List;

/** Device-aware receive defaults based on Great Scott Gadgets' published HackRF guidance. */
public final class HackRfTuningPolicy {
    private static final List<Integer> HACKRF_NATIVE_RATES = List.of(8_000_000, 10_000_000, 16_000_000, 20_000_000);
    private static final List<Integer> GENERIC_RATES = List.of(2_000_000, 4_000_000, 8_000_000, 10_000_000);

    private HackRfTuningPolicy() {}

    public static boolean appliesTo(SdrDevice device) {
        return device != null && "hackrf".equalsIgnoreCase(device.driver());
    }

    public static List<Integer> receiveSampleRates(SdrDevice device) {
        return appliesTo(device) ? HACKRF_NATIVE_RATES : GENERIC_RATES;
    }

    public static int recommendedReceiveSampleRate(SdrDevice device) {
        return appliesTo(device) ? 8_000_000 : 2_000_000;
    }

    public static String guidance(SdrDevice device) {
        if (!appliesTo(device)) return "Choose a rate supported by the selected radio.";
        return "HackRF receives at a native 8–20 MS/s; SDR-Pole filters and decimates narrow channels in software.";
    }
}
