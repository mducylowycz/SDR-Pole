package app.sdrpole.plugin;

import java.time.Instant;
import java.util.Map;

public record DecoderEvent(String type, Instant timestamp, Map<String, Object> fields) {}
