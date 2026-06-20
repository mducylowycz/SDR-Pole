package app.sdrpole.plugin;

import java.nio.FloatBuffer;

public interface DecoderSession extends AutoCloseable {
    void acceptIq(FloatBuffer interleavedIq) throws DecoderException;
    @Override void close();
}
