package app.sdrpole.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Safe wrapper for HackRF's firmware-assisted sweep mode. RF amp and antenna power are always forced off. */
public final class HackRfSweepService {
    public boolean available() {
        try { return new ProcessBuilder("hackrf_sweep", "-h").start().waitFor(3, TimeUnit.SECONDS); }
        catch (Exception ignored) { return false; }
    }

    public List<Peak> sweep(long startHz, long endHz, int binWidthHz, double thresholdDb) throws IOException {
        if (startHz < 1_000_000 || endHz > 6_000_000_000L || endHz <= startHz)
            throw new IllegalArgumentException("HackRF sweep range must be between 1 and 6000 MHz");
        int width = Math.max(2_445, Math.min(5_000_000, binWidthHz));
        var command = List.of("hackrf_sweep", "-f", (startHz / 1_000_000) + ":" + ((endHz + 999_999) / 1_000_000),
                "-w", Integer.toString(width), "-l", "16", "-g", "16", "-a", "0", "-p", "0", "-1");
        var process = new ProcessBuilder(command).redirectErrorStream(true).start();
        var captured = new ByteArrayOutputStream();
        var readFailure = new java.util.concurrent.atomic.AtomicReference<IOException>();
        var reader = Thread.startVirtualThread(() -> {
            try (var input = process.getInputStream()) { input.transferTo(captured); }
            catch (IOException problem) { readFailure.set(problem); }
        });
        try {
            if (!process.waitFor(Duration.ofSeconds(45).toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly(); throw new IOException("HackRF hardware sweep timed out");
            }
            reader.join();
        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new IOException("HackRF sweep interrupted", interrupted); }
        if (readFailure.get() != null) throw readFailure.get();
        var output = captured.toString(StandardCharsets.UTF_8);
        if (process.exitValue() != 0) throw new IOException(friendlyFailure(output));
        return parse(output, thresholdDb, 250);
    }

    static List<Peak> parse(String csv, double thresholdDb, int limit) throws IOException {
        var bins = new ArrayList<Peak>();
        try (var lines = new BufferedReader(new StringReader(csv))) {
            for (String line; (line = lines.readLine()) != null;) {
                var fields = line.split(",");
                if (fields.length < 7) continue;
                try {
                    long low = Long.parseLong(fields[2].trim());
                    long binWidth = Long.parseLong(fields[4].trim());
                    int samples = Integer.parseInt(fields[5].trim());
                    for (int index = 0; index < samples && index + 6 < fields.length; index++)
                        bins.add(new Peak(low + Math.round((index + .5) * binWidth), Double.parseDouble(fields[index + 6].trim()), binWidth));
                } catch (NumberFormatException ignored) { }
            }
        }
        if (bins.isEmpty()) return List.of();
        var levels = bins.stream().mapToDouble(Peak::powerDb).sorted().toArray();
        double noise = levels[(int) (levels.length * .35)];
        var strongest = bins.stream().filter(peak -> peak.powerDb() >= noise + Math.max(6, thresholdDb))
                .sorted(Comparator.comparingDouble(Peak::powerDb).reversed()).toList();
        var merged = new ArrayList<Peak>();
        for (var peak : strongest) {
            if (merged.stream().noneMatch(existing -> Math.abs(existing.frequencyHz() - peak.frequencyHz()) <= Math.max(existing.binWidthHz(), peak.binWidthHz()) * 2))
                merged.add(peak);
            if (merged.size() >= limit) break;
        }
        return List.copyOf(merged);
    }

    private static String friendlyFailure(String output) {
        if (output.contains("No HackRF boards found")) return "HackRF is not available; reconnect it and refresh Radios";
        if (output.contains("resource busy") || output.contains("HACKRF_ERROR_BUSY")) return "HackRF is busy; stop the current receiver and try again";
        var first = output.lines().filter(line -> !line.isBlank()).findFirst().orElse("hardware sweep failed");
        return "HackRF sweep failed: " + first;
    }

    public record Peak(long frequencyHz, double powerDb, long binWidthHz) {}
}
