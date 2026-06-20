package app.sdrpole.core;

/** Common receive modes exposed by the general-purpose SDR tuner. */
public enum DemodulationMode {
    NFM("Narrow FM"),
    WFM("Broadcast FM"),
    AM("AM"),
    USB("Upper sideband"),
    LSB("Lower sideband"),
    CW("CW");

    private final String label;

    DemodulationMode(String label) { this.label = label; }

    @Override public String toString() { return label; }
}
