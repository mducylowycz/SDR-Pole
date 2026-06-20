package app.sdrpole.desktop;

/** Keeps the JVM launcher from treating the main class as a modular JavaFX app. */
public final class Launcher {
    private Launcher() {}
    public static void main(String[] args) {
        SdrPoleApplication.main(args);
    }
}
