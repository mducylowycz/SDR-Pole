package app.sdrpole.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.zip.ZipInputStream;

/** Downloads the official JMBE Creator, verifies it, builds JMBE, and activates the validated JAR. */
public final class JmbeInstaller {
    public static final String VERSION = "1.0.9";
    private static final String RELEASE = "https://github.com/DSheirer/jmbe/releases/download/v1.0.9/";
    private static final Map<String, Artifact> ARTIFACTS = Map.of(
            "mac-x86_64", artifact("osx-x86_64", "1ba99ce79739ba0074e4d8ad1cd86f5f3653c130b8034589e75f1dde701cbff0"),
            "mac-aarch64", artifact("osx-aarch64", "cbb1e43c32acaa6b503aaf272f7589307c48e6793832b308c9ff44c1e5d025aa"),
            "linux-x86_64", artifact("linux-x86_64", "f84184ea54bb1c9db39d9c20684ca71aa8daa00a7d51ce7b6928a2fb9a726172"),
            "linux-aarch64", artifact("linux-aarch64", "f28f41ec1992d307a0f08d1adf3194c4b86f552deec04690b1f01b9aaf270270"),
            "windows-x86_64", artifact("windows-x86_64", "492b6c2a12e6d507db3ac7b29488b127533e95c55ece6a4875b261ddf7a8f21c"),
            "windows-aarch64", artifact("windows-aarch64", "ef8a9e41cc9cb13b51292181ce2b9233005498967dd8075c6ce08b2e61f7f372"));

    private final DecoderLibraryManager libraries;
    private final Path dataDirectory;

    public JmbeInstaller(DecoderLibraryManager libraries) {
        this(libraries, applicationDataDirectory());
    }

    JmbeInstaller(DecoderLibraryManager libraries, Path dataDirectory) {
        this.libraries = libraries;
        this.dataDirectory = dataDirectory;
    }

    public Path install(BiConsumer<String, Double> progress) throws Exception {
        var artifact = currentArtifact();
        var downloads = dataDirectory.resolve("downloads");
        var tools = dataDirectory.resolve("tools");
        var decoders = dataDirectory.resolve("decoders");
        Files.createDirectories(downloads);
        Files.createDirectories(tools);
        Files.createDirectories(decoders);
        var archive = downloads.resolve(artifact.fileName());

        progress.accept("Downloading official JMBE Creator " + VERSION, 0.02);
        if (!Files.isRegularFile(archive) || !sha256(archive).equals(artifact.sha256())) {
            download(artifact.uri(), archive, progress);
        }
        progress.accept("Verifying JMBE Creator checksum", 0.72);
        if (!sha256(archive).equals(artifact.sha256())) {
            Files.deleteIfExists(archive);
            throw new IOException("JMBE Creator checksum did not match the pinned release");
        }

        var creatorRoot = tools.resolve("jmbe-creator-" + VERSION + "-" + platformKey());
        if (!Files.isDirectory(creatorRoot)) {
            var staging = Files.createTempDirectory(tools, ".jmbe-");
            try {
                unzipSafely(archive, staging);
                try (var roots = Files.list(staging)) {
                    var extracted = roots.filter(Files::isDirectory).findFirst()
                            .orElseThrow(() -> new IOException("JMBE Creator archive was empty"));
                    Files.move(extracted, creatorRoot, StandardCopyOption.ATOMIC_MOVE);
                }
            } finally {
                deleteTree(staging);
            }
        }

        progress.accept("Building JMBE from the official tagged source", 0.78);
        var launcher = creatorRoot.resolve("bin").resolve(isWindows() ? "creator.bat" : "creator");
        if (!isWindows()) {
            // ZIP does not preserve Unix mode bits. The Creator includes its own java/javac runtime.
            try (var executables = Files.list(creatorRoot.resolve("bin"))) {
                for (var executable : executables.filter(Files::isRegularFile).toList()) {
                    if (!executable.toFile().setExecutable(true, true)) {
                        throw new IOException("Could not make JMBE Creator executable: " + executable.getFileName());
                    }
                }
            }
        }
        var command = isWindows()
                ? new String[]{"cmd.exe", "/c", launcher.toString()}
                : new String[]{launcher.toString()};
        var process = new ProcessBuilder(command).directory(decoders.toFile()).redirectErrorStream(true).start();
        var output = new String(process.getInputStream().readAllBytes());
        if (!process.waitFor(Duration.ofMinutes(5).toMillis(), TimeUnit.MILLISECONDS)) {
            process.destroyForcibly();
            throw new IOException("JMBE build timed out");
        }
        if (process.exitValue() != 0) throw new IOException("JMBE build failed: " + tail(output));

        var jar = decoders.resolve("jmbe-" + VERSION + ".jar");
        var creatorOutput = dataDirectory.resolve("jmbe-" + VERSION + ".jar");
        if (!Files.isRegularFile(jar) && Files.isRegularFile(creatorOutput)) {
            Files.move(creatorOutput, jar, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        }
        if (!Files.isRegularFile(jar)) {
            throw new IOException("JMBE Creator finished but did not produce " + jar.getFileName() + ": " + tail(output));
        }
        libraries.selectJmbe(jar);
        progress.accept("JMBE installed and validated", 1.0);
        return jar;
    }

    public static Artifact currentArtifact() {
        var artifact = ARTIFACTS.get(platformKey());
        if (artifact == null) throw new IllegalStateException("JMBE Creator is unavailable for " + platformKey());
        return artifact;
    }

    static String platformKey() {
        var os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        var arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        var family = os.contains("mac") ? "mac" : os.contains("win") ? "windows" : os.contains("linux") ? "linux" : os;
        var normalizedArch = arch.contains("aarch64") || arch.contains("arm64") ? "aarch64" : "x86_64";
        return family + "-" + normalizedArch;
    }

    public static Path applicationDataDirectory() {
        var home = Path.of(System.getProperty("user.home"));
        return switch (platformKey().split("-")[0]) {
            case "mac" -> home.resolve("Library/Application Support/SDR-Pole");
            case "windows" -> Path.of(System.getenv().getOrDefault("APPDATA", home.resolve("AppData/Roaming").toString()), "SDR-Pole");
            default -> Path.of(System.getenv().getOrDefault("XDG_DATA_HOME", home.resolve(".local/share").toString()), "sdr-pole");
        };
    }

    private static Artifact artifact(String platform, String sha256) {
        var file = "jmbe-creator-" + platform + "-v" + VERSION + ".zip";
        return new Artifact(file, URI.create(RELEASE + file), sha256);
    }

    private static void download(URI uri, Path destination, BiConsumer<String, Double> progress) throws IOException {
        var staging = Files.createTempFile(destination.getParent(), ".jmbe-download-", ".zip");
        try {
            var connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(120_000);
            connection.setInstanceFollowRedirects(true);
            var length = Math.max(1, connection.getContentLengthLong());
            try (InputStream input = connection.getInputStream(); var output = Files.newOutputStream(staging)) {
                var buffer = new byte[64 * 1024];
                long total = 0;
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    output.write(buffer, 0, read);
                    total += read;
                    progress.accept("Downloading JMBE Creator", Math.min(.7, .7 * total / length));
                }
            }
            Files.move(staging, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(staging);
        }
    }

    private static void unzipSafely(Path archive, Path destination) throws IOException {
        try (var zip = new ZipInputStream(Files.newInputStream(archive))) {
            var entry = zip.getNextEntry();
            while (entry != null) {
                var target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination)) throw new IOException("Unsafe path in JMBE archive");
                if (entry.isDirectory()) Files.createDirectories(target);
                else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                }
                entry = zip.getNextEntry();
            }
        }
    }

    private static String sha256(Path file) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(file)) {
            var buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (var path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    private static boolean isWindows() { return platformKey().startsWith("windows-"); }
    private static String tail(String output) { return output.length() <= 500 ? output : output.substring(output.length() - 500); }

    public record Artifact(String fileName, URI uri, String sha256) {}
}
