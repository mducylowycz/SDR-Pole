package app.sdrpole.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Local, append-only operational audit log with sensitive-field redaction and size rotation. */
public final class AuditLog {
    private static final long MAX_BYTES = 5L * 1024 * 1024;
    private static final Set<String> SENSITIVE = Set.of("password", "secret", "token", "credential", "serial", "latitude", "longitude", "location");
    private final Path file;
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());

    public AuditLog() { this(JmbeInstaller.applicationDataDirectory().resolve("audit/events.jsonl")); }
    AuditLog(Path file) { this.file = file.toAbsolutePath().normalize(); }

    public synchronized void record(String category, String action, String outcome, Map<String, ?> details) {
        try {
            Files.createDirectories(file.getParent());
            if (Files.isRegularFile(file) && Files.size(file) >= MAX_BYTES)
                Files.move(file, file.resolveSibling("events.previous.jsonl"), StandardCopyOption.REPLACE_EXISTING);
            var safe = new LinkedHashMap<String, String>();
            if (details != null) details.forEach((key, value) -> safe.put(key, isSensitive(key) ? "[REDACTED]" : String.valueOf(value)));
            var line = json.writeValueAsString(new AuditEvent(Instant.now(), clean(category), clean(action), clean(outcome), Map.copyOf(safe))) + System.lineSeparator();
            Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            restrict(file);
        } catch (IOException ignored) { /* Logging must not break radio operation. Diagnostics reports availability. */ }
    }

    public Path path() { return file; }
    private static boolean isSensitive(String key) { var lower = key.toLowerCase(); return SENSITIVE.stream().anyMatch(lower::contains); }
    private static String clean(String text) { return text == null ? "" : text.replaceAll("[\\r\\n\\t]", " "); }
    private static void restrict(Path path) {
        try { Files.setPosixFilePermissions(path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)); }
        catch (UnsupportedOperationException | IOException ignored) { }
    }
}
