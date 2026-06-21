package app.sdrpole.core.p25;

/** Human-readable talkgroup data supplied by an authenticated directory. */
public record P25Talkgroup(String systemName, int id, String alias, String description,
                           String category, int encryption) {
    public P25Talkgroup {
        if (systemName == null || systemName.isBlank()) throw new IllegalArgumentException("System name is required");
        if (id < 0 || id > 65_535) throw new IllegalArgumentException("Talkgroup ID must be 0–65535");
        alias = alias == null ? "" : alias.strip();
        description = description == null ? "" : description.strip();
        category = category == null ? "" : category.strip();
        if (encryption < 0 || encryption > 2) throw new IllegalArgumentException("Encryption state must be 0–2");
    }

    public boolean fullyEncrypted() { return encryption == 2; }
}
