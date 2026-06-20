package app.sdrpole.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/** Discovers all locally installed SoapySDR devices, with a HackRF fallback. */
public final class DeviceDiscoveryService {
    private final Duration timeout;
    private volatile String lastDiagnostic = "Discovery has not run";

    public DeviceDiscoveryService() { this(Duration.ofSeconds(8)); }
    public DeviceDiscoveryService(Duration timeout) { this.timeout = timeout; }

    public List<SdrDevice> discover() {
        var soapy = run(resolveExecutable("SoapySDRUtil"), "--find");
        var devices = parseSoapyFind(soapy.output());
        if (!devices.isEmpty()) {
            lastDiagnostic = "SoapySDR found " + devices.size() + " device(s)";
            return devices;
        }

        var hackrf = run(resolveExecutable("hackrf_info"));
        if (hackrf.exitCode() == 0 && hackrf.output().contains("HackRF")) {
            var serial = valueAfter(hackrf.output(), "Serial number:");
            lastDiagnostic = "HackRF found through libhackrf fallback";
            return List.of(new SdrDevice(
                    "hackrf:" + (serial.isBlank() ? "0" : serial), "hackrf", "HackRF One",
                    serial, true, Map.of("provider", "libhackrf")));
        }
        lastDiagnostic = friendlyDiagnostic(soapy, hackrf);
        return List.of();
    }

    public String lastDiagnostic() { return lastDiagnostic; }

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
            var builder = new ProcessBuilder(command).redirectErrorStream(true);
            builder.environment().merge("PATH", "/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin",
                    (current, required) -> required + ":" + current);
            var process = builder.start();
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

    private static String resolveExecutable(String name) {
        for (var directory : List.of("/usr/local/bin", "/opt/homebrew/bin", "/usr/bin", "/bin")) {
            var candidate = Path.of(directory, name);
            if (Files.isExecutable(candidate)) return candidate.toString();
        }
        return name;
    }

    private static String friendlyDiagnostic(CommandResult soapy, CommandResult hackrf) {
        var combined = (soapy.output() + "\n" + hackrf.output()).toLowerCase();
        if (combined.contains("no hackrf boards found") || combined.contains("no devices found")) {
            return "macOS does not currently see an SDR on USB. Reconnect it directly, try another data cable/port, and refresh.";
        }
        if (combined.contains("access denied") || combined.contains("busy") || combined.contains("claim")) {
            return "The SDR is being used by another application. Close SDRTrunk/GopherTrunk and refresh.";
        }
        if (soapy.exitCode() == -1 && hackrf.exitCode() == -1) {
            return "SDR driver tools are missing. Install the recommended hardware package from Devices.";
        }
        return "No compatible SDR was detected. Open Diagnostics for cable, driver, and process checks.";
    }

    private static String valueAfter(String text, String marker) {
        return text.lines().map(String::trim).filter(s -> s.startsWith(marker))
                .map(s -> s.substring(marker.length()).trim()).findFirst().orElse("");
    }

    private record CommandResult(int exitCode, String output) {}
}
