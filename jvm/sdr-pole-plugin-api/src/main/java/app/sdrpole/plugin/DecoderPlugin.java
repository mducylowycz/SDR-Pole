package app.sdrpole.plugin;

import java.util.Set;

/** Stable boundary implemented by optional decoder packages. */
public interface DecoderPlugin extends AutoCloseable {
    String id();
    String displayName();
    String version();
    Set<DecoderCapability> capabilities();
    DecoderSession open(DecoderContext context) throws DecoderException;
    @Override default void close() throws Exception {}
}
