package app.sdrpole.core;

import java.util.ArrayList;
import java.util.List;

public final class RfSafetyPolicy {
    private RfSafetyPolicy() {}

    public static Decision sanitize(SdrDevice device, long frequencyHz, RfFrontendSettings requested) {
        var profile = RfSafetyProfile.forDevice(device);
        if (frequencyHz < profile.minimumFrequencyHz() || frequencyHz > profile.maximumFrequencyHz()) {
            throw new IllegalArgumentException("Frequency is outside " + profile.hardware() + " receive limits");
        }
        var warnings = new ArrayList<String>();
        double lna = profile.lna().clampAndStep(requested.lnaGainDb());
        double vga = profile.vga().clampAndStep(requested.vgaGainDb());
        if (lna != requested.lnaGainDb()) warnings.add("LNA gain was clamped to " + lna + " dB");
        if (vga != requested.vgaGainDb()) warnings.add("VGA gain was clamped to " + vga + " dB");

        boolean amp = requested.rfAmplifier() && profile.rfAmplifierSupported() && requested.highRiskConfirmed();
        boolean power = requested.antennaPower() && profile.antennaPowerSupported() && requested.highRiskConfirmed();
        if (requested.rfAmplifier() && !amp) warnings.add("RF amplifier stayed off because high-risk controls were not confirmed");
        if (requested.antennaPower() && !power) warnings.add("Antenna power stayed off because high-risk controls were not confirmed");
        return new Decision(new RfFrontendSettings(lna, vga, amp, power, requested.highRiskConfirmed()),
                List.copyOf(warnings), profile);
    }

    public record Decision(RfFrontendSettings settings, List<String> warnings, RfSafetyProfile profile) {}
}
