package app.sdrpole.core.p25;

import app.sdrpole.core.JmbeInstaller;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Atomic local persistence for user-created P25 site configurations. */
public final class P25SystemStore {
    private final Path file;
    private final ObjectMapper mapper = new ObjectMapper();

    public P25SystemStore() {
        this(JmbeInstaller.applicationDataDirectory().resolve("systems/p25-sites.json"));
    }

    P25SystemStore(Path file) { this.file = file; }

    public List<P25SystemConfig> load() throws IOException {
        if (!Files.isRegularFile(file)) return List.of();
        return List.copyOf(mapper.readValue(file.toFile(), new TypeReference<List<P25SystemConfig>>() {}));
    }

    public void save(List<P25SystemConfig> systems) throws IOException {
        Files.createDirectories(file.getParent());
        var staging = Files.createTempFile(file.getParent(), ".p25-sites-", ".json");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(staging.toFile(), List.copyOf(systems));
            Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(staging); }
    }
}
