package app.sdrpole.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {
    @Test void writesJsonLinesAndRedactsSensitiveFields(@TempDir Path directory) throws Exception {
        var log = new AuditLog(directory.resolve("events.jsonl"));
        log.record("scanner", "start\nscan", "success", Map.of("ranges", 2, "latitude", 41.2, "deviceSerial", "secret-id"));
        var content = Files.readString(log.path());
        assertTrue(content.contains("start scan"));
        assertTrue(content.contains("[REDACTED]"));
        assertFalse(content.contains("41.2"));
        assertFalse(content.contains("secret-id"));
    }
}
