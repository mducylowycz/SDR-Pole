package app.sdrpole.core.hackrf;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Read-only hardware probe. It never flashes firmware, enables TX, or changes clock/antenna state. */
public final class HackRfProfileService {
    private static final Pattern API = Pattern.compile(".*\\(API:([^\\)]+)\\).*");
    private final Duration timeout;

    public HackRfProfileService() { this(Duration.ofSeconds(6)); }
    HackRfProfileService(Duration timeout) { this.timeout = timeout; }

    public List<HackRfProfile> probe() {
        var result = run(tool("hackrf_info"));
        if (result.exitCode != 0) return List.of();
        return parse(result.output, exists("hackrf_sweep"), exists("hackrf_clock"), exists("hackrf_operacake"));
    }

    public static List<HackRfProfile> parse(String output, boolean sweep, boolean clock, boolean operaCake) {
        var tools = value(output, "hackrf_info version:");
        var library = value(output, "libhackrf version:");
        var blocks = output.split("(?=Found HackRF)");
        var profiles = new ArrayList<HackRfProfile>();
        for (var block : blocks) {
            if (!block.contains("Found HackRF")) continue;
            var serial = value(block, "Serial number:");
            var boardText = value(block, "Board ID Number:");
            var firmware = value(block, "Firmware Version:");
            var apiMatcher = API.matcher(firmware);
            var api = apiMatcher.matches() ? apiMatcher.group(1).trim() : "unknown";
            var cleanFirmware = firmware.replaceAll("\\s*\\(API:[^\\)]+\\).*", "").trim();
            var findings = new ArrayList<String>();
            if (!tools.isBlank() && !cleanFirmware.isBlank() && !cleanFirmware.startsWith(tools))
                findings.add("Host tools and firmware versions differ; update them together before troubleshooting RF behavior.");
            if (!sweep) findings.add("hackrf_sweep is missing; native wideband discovery is unavailable.");
            findings.add("Receive-only policy active in SDR-Pole; transmit paths are not exposed.");
            profiles.add(new HackRfProfile(serial, parenthesized(boardText, "HackRF"), beforeParenthesis(boardText),
                    cleanFirmware, api, tools, library, value(block, "Part ID Number:"), sweep, clock, operaCake, findings));
        }
        return List.copyOf(profiles);
    }

    private Result run(String... command) {
        try {
            var process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) { process.destroyForcibly(); return new Result(-1, "Timed out"); }
            return new Result(process.exitValue(), new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException problem) {
            if (problem instanceof InterruptedException) Thread.currentThread().interrupt();
            return new Result(-1, problem.getMessage() == null ? "Unavailable" : problem.getMessage());
        }
    }

    private static boolean exists(String name) { return Files.isExecutable(Path.of(tool(name))); }
    private static String tool(String name) {
        for (var directory : List.of("/usr/local/bin", "/opt/homebrew/bin", "/usr/bin", "/bin")) {
            var candidate = Path.of(directory, name); if (Files.isExecutable(candidate)) return candidate.toString();
        }
        return name;
    }
    private static String value(String text, String marker) {
        return text.lines().map(String::trim).filter(line -> line.startsWith(marker))
                .map(line -> line.substring(marker.length()).trim()).findFirst().orElse("");
    }
    private static String parenthesized(String text, String fallback) {
        int start = text.indexOf('('), end = text.lastIndexOf(')'); return start >= 0 && end > start ? text.substring(start + 1, end) : fallback;
    }
    private static String beforeParenthesis(String text) { int index = text.indexOf('('); return (index < 0 ? text : text.substring(0, index)).trim(); }
    private record Result(int exitCode, String output) {}
}
