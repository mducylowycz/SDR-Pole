package app.sdrpole.core;

import app.sdrpole.plugin.DecoderCapability;
import java.net.URI;
import java.util.Set;

public record DecoderCatalogEntry(
        String id,
        String name,
        String summary,
        String license,
        URI projectUrl,
        Set<DecoderCapability> capabilities,
        Availability availability) {
    public DecoderCatalogEntry { capabilities = Set.copyOf(capabilities); }

    public enum Availability { BUILT_IN, READY_TO_INSTALL, REQUIRES_PACKAGE, COMING_SOON }
}
