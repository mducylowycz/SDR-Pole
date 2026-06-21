package app.sdrpole.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.prefs.Preferences;

/** Installs and validates an external P25 Phase 1/2 engine without mixing it into UI or DSP code. */
public final class P25EngineManager {
    private final Preferences preferences = Preferences.userNodeForPackage(P25EngineManager.class);

    public Path installGopherTrunk(Path sourceBinary, String version) throws IOException {
        if (!Files.isRegularFile(sourceBinary) || !Files.isExecutable(sourceBinary))
            throw new IOException("GopherTrunk executable was not found or is not executable");
        byte[] header = new byte[4];
        try (var input = Files.newInputStream(sourceBinary)) {
            if (input.read(header) != 4) throw new IOException("Selected executable is empty");
        }
        if (header.length < 4 || !isMachO(header)) throw new IOException("Selected file is not a supported macOS executable");
        var directory = JmbeInstaller.applicationDataDirectory().resolve("decoders/gophertrunk-" + version);
        Files.createDirectories(directory);
        var staging = directory.resolve(".gophertrunk.tmp");
        var target = directory.resolve("gophertrunk");
        Files.copy(sourceBinary, staging, StandardCopyOption.REPLACE_EXISTING);
        staging.toFile().setExecutable(true, true);
        Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        preferences.put("p25.engine.path", target.toAbsolutePath().normalize().toString());
        preferences.put("p25.engine.version", version);
        return target;
    }

    public Optional<Path> enginePath() {
        var value = preferences.get("p25.engine.path", "");
        if (!value.isBlank()) {
            var path = Path.of(value);
            if (Files.isRegularFile(path) && Files.isExecutable(path)) return Optional.of(path);
        }
        var managed = JmbeInstaller.applicationDataDirectory().resolve("decoders/gophertrunk-0.4.8/gophertrunk");
        return Files.isRegularFile(managed) && Files.isExecutable(managed) ? Optional.of(managed) : Optional.empty();
    }

    public String version() { return preferences.get("p25.engine.version", enginePath().isPresent() ? "0.4.8" : "unknown"); }

    private static boolean isMachO(byte[] bytes) {
        int magic = ((bytes[0] & 255) << 24) | ((bytes[1] & 255) << 16) | ((bytes[2] & 255) << 8) | (bytes[3] & 255);
        return magic == 0xfeedfacf || magic == 0xcffaedfe || magic == 0xcafebabe;
    }
}
