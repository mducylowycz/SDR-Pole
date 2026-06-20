package app.sdrpole.core;

import java.net.URI;

public record DecoderPackage(
        String id,
        String name,
        String version,
        URI downloadUrl,
        String sha256,
        String license,
        String minimumAppVersion) {}
