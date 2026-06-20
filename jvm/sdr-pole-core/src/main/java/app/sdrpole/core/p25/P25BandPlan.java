package app.sdrpole.core.p25;

public record P25BandPlan(int identifier, long baseFrequencyHz, int channelSpacingHz, long transmitOffsetHz) {
    public P25BandPlan {
        if (identifier < 0 || identifier > 15) throw new IllegalArgumentException("Identifier must be from 0 to 15");
        if (baseFrequencyHz <= 0) throw new IllegalArgumentException("Base frequency must be positive");
        if (channelSpacingHz <= 0) throw new IllegalArgumentException("Channel spacing must be positive");
    }

    public long downlinkHz(int channelNumber) {
        if (channelNumber < 0 || channelNumber > 0xFFF) throw new IllegalArgumentException("Channel number must be 12-bit");
        return Math.addExact(baseFrequencyHz, Math.multiplyExact((long) channelSpacingHz, channelNumber));
    }

    public long uplinkHz(int channelNumber) { return Math.addExact(downlinkHz(channelNumber), transmitOffsetHz); }
}
