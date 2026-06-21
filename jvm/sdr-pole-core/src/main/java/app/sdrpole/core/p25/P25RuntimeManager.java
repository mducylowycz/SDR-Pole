package app.sdrpole.core.p25;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/** Owns the local decoder process so ordinary users never manage a daemon or terminal. */
public final class P25RuntimeManager implements AutoCloseable {
    private Process process;
    private Path logFile;

    public synchronized void start(Path engine, P25RuntimePlan plan) throws IOException {
        if (!Files.isRegularFile(engine) || !Files.isExecutable(engine))
            throw new IOException("Install the P25 engine from Decoder packages first");
        stop();
        logFile = plan.configFile().getParent().resolve("logs/engine.log");
        Files.createDirectories(logFile.getParent());
        process = new ProcessBuilder(engine.toString(), "-headless", "-config", plan.configFile().toString())
                .directory(plan.configFile().getParent().toFile())
                .redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile())).start();
        try {
            if (process.waitFor(700, TimeUnit.MILLISECONDS) && process.exitValue() != 0)
                throw new IOException("The P25 engine stopped during startup; open Diagnostics for its log");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IOException("P25 startup was interrupted", interrupted);
        }
    }

    public synchronized boolean running() { return process != null && process.isAlive(); }
    public synchronized Optional<Path> logFile() { return Optional.ofNullable(logFile); }

    public synchronized void stop() {
        if (process == null) return;
        process.destroy();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
        process = null;
    }

    @Override public void close() { stop(); }
}
