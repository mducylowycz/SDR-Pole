package app.sdrpole.core;

public enum ScanSpeed {
    TURBO("Turbo", 175), FAST("Fast", 300), BALANCED("Balanced", 500), DEEP("Deep", 900);

    private final String label;
    private final int dwellMilliseconds;
    ScanSpeed(String label, int dwellMilliseconds) { this.label = label; this.dwellMilliseconds = dwellMilliseconds; }
    public int dwellMilliseconds() { return dwellMilliseconds; }
    @Override public String toString() { return label + " · " + dwellMilliseconds + " ms/window"; }
}
