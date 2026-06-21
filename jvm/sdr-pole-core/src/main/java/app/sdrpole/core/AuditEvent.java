package app.sdrpole.core;

import java.time.Instant;
import java.util.Map;

public record AuditEvent(Instant timestamp, String category, String action, String outcome,
                         Map<String, String> details) {}
