package app.sdrpole.core;

/** Requested receive front-end controls. High-risk controls require explicit confirmation. */
public record RfFrontendSettings(
        double lnaGainDb,
        double vgaGainDb,
        boolean rfAmplifier,
        boolean antennaPower,
        boolean highRiskConfirmed) {

    public static RfFrontendSettings safeDefaults(SdrDevice device) {
        if (device != null && "hackrf".equalsIgnoreCase(device.driver())) {
            return new RfFrontendSettings(16, 16, false, false, false);
        }
        return new RfFrontendSettings(0, 0, false, false, false);
    }
}
