package app.sdrpole.core;

public interface ReceiverListener {
    default void onStatus(String message) {}
    default void onSpectrum(float[] powerDb) {}
    default void onLevel(double rms) {}
    default void onError(String friendlyMessage, Throwable cause) {}
}
