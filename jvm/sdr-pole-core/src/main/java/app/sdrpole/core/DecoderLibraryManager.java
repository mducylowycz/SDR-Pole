package app.sdrpole.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;

/** Validates and remembers separately licensed decoder libraries selected by the user. */
public final class DecoderLibraryManager {
    private static final String JMBE_CLASS = "jmbe/JMBEAudioLibrary.class";
    private final Preferences preferences = Preferences.userNodeForPackage(DecoderLibraryManager.class);

    public void selectJmbe(Path jar) throws IOException {
        var normalized = jar.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized)) throw new IOException("The selected JMBE file does not exist");
        try (var archive = new JarFile(normalized.toFile())) {
            if (archive.getJarEntry(JMBE_CLASS) == null) {
                throw new IOException("This JAR does not contain the JMBE audio library");
            }
        }
        preferences.put("jmbe.path", normalized.toString());
    }

    public Optional<Path> jmbePath() {
        var value = preferences.get("jmbe.path", "");
        if (value.isBlank()) return Optional.empty();
        var path = Path.of(value);
        return Files.isRegularFile(path) ? Optional.of(path) : Optional.empty();
    }

    public void clearJmbe() { preferences.remove("jmbe.path"); }
}
