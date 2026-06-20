package app.sdrpole.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Discovers all locally installed SoapySDR devices, with a HackRF fallback. */
public final class DeviceDiscoveryService {
    private final Duration timeout;

    public DeviceDiscoveryService() { this(Duration.ofSeconds(8)); }
    public DeviceDiscoveryService(Duration timeout) { this.timeout = timeout; }

    public List<SdrDevice> discover() {
        var soapy = run("SoapySDRUtil", "--find");
        var devices = parseSoapyFind(soapy.output());
        if (!devices.isEmpty()) return devices;

        var hackrf = run("hackrf_info");
        if (hackrf.exitCode() == 0 && hackrf.output().contains("HackRF")) {
            var serial = valueAfter(hackrf.output(), "Serial number:");
            return List.of(new SdrDevice(
                    "hackrf:" + (serial.isBlank() ? "0" : serial), "hackrf", "HackRF One",
                    serial, true, Map.of("provider", "libhackrf")));
        }
        return List.of();
    }

    public static List<SdrDevice> parseSoapyFind(String output) {
        var result = new ArrayList<SdrDevice>();
        Map<String, String> block = null;
        for (var raw : output.lines().toList()) {
            var line = raw.trim();
            if (line.matches("Found device \\d+.*")) {
                if (block != null) addDevice(result, block);
                block = new LinkedHashMap<>();
            } else if (block != null && line.contains("=")) {
                var parts = line.split("=", 2);
                block.put(parts[0].trim(), parts[1].trim());
            }
        }
        if (block != null) addDevice(result, block);
        return List.copyOf(result);
    }

    private static void addDevice(List<SdrDevice> result, Map<String, String> data) {
        var driver = data.getOrDefault("driver", "unknown");
        var serial = data.getOrDefault("serial", data.getOrDefault("device_id", Integer.toString(result.size())));
        var label = data.getOrDefault("label", friendlyDriver(driver));
        result.add(new SdrDevice(driver + ":" + serial, driver, label, serial, true, data));
    }

    private static String friendlyDriver(String driver) {
        return switch (driver.toLowerCase()) {
            case "hackrf" -> "HackRF One";
            case "rtlsdr" -> "RTL-SDR";
            case "airspy" -> "Airspy";
            case "sdrplay" -> "SDRplay";
            case "lime" -> "LimeSDR";
            case "plutosdr" -> "PlutoSDR";
            default -> driver;
        };
    }

    private CommandResult run(String... command) {
        try {
            var process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                return new CommandResult(-1, "Timed out");
            }
            return new CommandResult(process.exitValue(),
                    new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return new CommandResult(-1, e.getMessage() == null ? "Unavailable" : e.getMessage());
        }
    }

    private static String valueAfter(String text, String marker) {
        return text.lines().map(String::trim).filter(s -> s.startsWith(marker))
                .map(s -> s.substring(marker.length()).trim()).findFirst().orElse("");
    }

    private record CommandResult(int exitCode, String output) {}
}
