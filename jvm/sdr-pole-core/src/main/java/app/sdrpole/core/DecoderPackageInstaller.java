package app.sdrpole.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Downloads to a staging file, verifies SHA-256, then atomically activates it. */
public final class DecoderPackageInstaller {
    private final Path pluginDirectory;

    public DecoderPackageInstaller(Path pluginDirectory) {
        this.pluginDirectory = pluginDirectory.toAbsolutePath().normalize();
    }

    public Path install(DecoderPackage pkg) throws IOException {
        Files.createDirectories(pluginDirectory);
        var safeName = pkg.id().replaceAll("[^a-zA-Z0-9._-]", "_") + "-" + pkg.version() + ".jar";
        var destination = pluginDirectory.resolve(safeName).normalize();
        if (!destination.startsWith(pluginDirectory)) throw new IOException("Unsafe plugin path");
        var staging = Files.createTempFile(pluginDirectory, ".download-", ".jar");
        try {
            var connection = (HttpURLConnection) pkg.downloadUrl().toURL().openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(60_000);
            connection.setInstanceFollowRedirects(true);
            try (InputStream input = connection.getInputStream()) {
                Files.copy(input, staging, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            var actual = sha256(staging);
            if (!actual.equalsIgnoreCase(pkg.sha256())) {
                throw new IOException("Decoder checksum mismatch for " + pkg.name());
            }
            return Files.move(staging, destination,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(staging);
        }
    }

    private static String sha256(Path file) throws IOException {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            try (var input = Files.newInputStream(file)) {
                input.transferTo(new java.io.OutputStream() {
                    @Override public void write(int b) { digest.update((byte) b); }
                    @Override public void write(byte[] b, int off, int len) { digest.update(b, off, len); }
                });
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
