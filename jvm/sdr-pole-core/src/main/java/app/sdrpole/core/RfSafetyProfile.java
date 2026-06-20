package app.sdrpole.core;

/** Vendor-derived receive limits used before any value reaches a native driver. */
public record RfSafetyProfile(
        String hardware,
        double minimumFrequencyHz,
        double maximumFrequencyHz,
        double maximumInputDbm,
        GainRange lna,
        GainRange vga,
        boolean rfAmplifierSupported,
        boolean antennaPowerSupported,
        String warning) {

    public static RfSafetyProfile forDevice(SdrDevice device) {
        if (device != null && "hackrf".equalsIgnoreCase(device.driver())) {
            return new RfSafetyProfile("HackRF One", 1_000_000, 6_000_000_000L, -5,
                    new GainRange(0, 40, 8), new GainRange(0, 62, 2), true, true,
                    "Maximum input is -5 dBm. Use an external attenuator/filter near transmitters. " +
                            "RF amp and antenna power remain off unless explicitly confirmed.");
        }
        return new RfSafetyProfile(device == null ? "Unknown SDR" : device.label(), 0, Double.MAX_VALUE,
                Double.NaN, new GainRange(0, 0, 1), new GainRange(0, 0, 1), false, false,
                "SDR-Pole will use conservative generic gain. Check the device manufacturer's RF limits.");
    }

    public record GainRange(double minimum, double maximum, double step) {
        public double clampAndStep(double value) {
            var clamped = Math.max(minimum, Math.min(maximum, value));
            return minimum + Math.round((clamped - minimum) / step) * step;
        }
    }
}
