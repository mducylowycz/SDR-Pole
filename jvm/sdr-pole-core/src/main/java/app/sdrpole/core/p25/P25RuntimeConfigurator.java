package app.sdrpole.core.p25;

import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.JmbeInstaller;
import app.sdrpole.core.SdrDevice;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Selects a nearby P25 site and emits a private, loopback-only GopherTrunk configuration. */
public final class P25RuntimeConfigurator {
    private final Path runtimeDirectory;

    public P25RuntimeConfigurator() {
        this(JmbeInstaller.applicationDataDirectory().resolve("runtime/gophertrunk"));
    }

    P25RuntimeConfigurator(Path runtimeDirectory) { this.runtimeDirectory = runtimeDirectory; }

    public P25RuntimePlan create(List<SdrDevice> devices, List<P25SystemConfig> systems,
                                 GeoPoint listeningLocation) throws IOException {
        if (devices == null || devices.stream().noneMatch(SdrDevice::available))
            throw new IllegalStateException("Connect an SDR and press Refresh first");
        if (systems == null || systems.isEmpty())
            throw new IllegalStateException("Load or add a P25 system first");

        var available = devices.stream().filter(SdrDevice::available).toList();
        var system = chooseSystem(systems, listeningLocation);
        var control = available.stream().min(Comparator.comparingInt(P25RuntimeConfigurator::deviceRank)).orElseThrow();
        var voice = available.stream().filter(device -> device != control).toList();
        int sampleRate = recommendedSampleRate(control);
        long center = system.controlFrequenciesHz().stream().mapToLong(Long::longValue).sum()
                / system.controlFrequenciesHz().size();
        int voiceTaps = Math.min(system.maxTrafficChannels(), control.driver().toLowerCase(Locale.ROOT).contains("hackrf") ? 4 : 2);

        Files.createDirectories(runtimeDirectory.resolve("data"));
        Files.createDirectories(runtimeDirectory.resolve("recordings"));
        Files.createDirectories(runtimeDirectory.resolve("logs"));
        var config = runtimeDirectory.resolve("config.yaml");
        var staging = Files.createTempFile(runtimeDirectory, ".config-", ".yaml");
        Files.writeString(staging, yaml(system, available, control, center, sampleRate, voiceTaps), StandardCharsets.UTF_8);
        try {
            Files.move(staging, config, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(staging); }

        var notes = new ArrayList<String>();
        notes.add(system.modulation() == P25SystemConfig.Modulation.LSM_SIMULCAST
                ? "Simulcast equalization and CQPSK demodulation enabled"
                : "C4FM control-channel demodulation selected");
        notes.add(voice.isEmpty()
                ? "One-radio mode: calls inside the sampled bandwidth use up to " + voiceTaps + " virtual voice tuners"
                : voice.size() + " additional radio(s) assigned to traffic channels");
        notes.add("Engine API is restricted to this computer (127.0.0.1)");
        return new P25RuntimePlan(system, control, voice, center, sampleRate, voiceTaps, config, notes);
    }

    static P25SystemConfig chooseSystem(List<P25SystemConfig> systems, GeoPoint location) {
        if (location == null) return systems.getFirst();
        return systems.stream().min(Comparator.comparingDouble(system ->
                system.location() == null ? Double.MAX_VALUE : location.distanceKmTo(system.location()))).orElseThrow();
    }

    static int recommendedSampleRate(SdrDevice device) {
        var driver = device.driver().toLowerCase(Locale.ROOT);
        if (driver.contains("hackrf")) return 8_000_000;
        if (driver.contains("airspy")) return 6_000_000;
        return 2_400_000;
    }

    private static int deviceRank(SdrDevice device) {
        var driver = device.driver().toLowerCase(Locale.ROOT);
        if (driver.contains("hackrf")) return 0;
        if (driver.contains("airspy")) return 1;
        return 2;
    }

    private static String yaml(P25SystemConfig system, List<SdrDevice> devices, SdrDevice control,
                               long center, int sampleRate, int voiceTaps) {
        var id = slug(system.systemName() + "-" + system.siteName());
        var out = new StringBuilder("""
                log:
                  level: info
                  format: json
                diagnostics:
                  verbose_errors: false
                api:
                  http_addr: "127.0.0.1:18080"
                  grpc_addr: "127.0.0.1:15051"
                  auth:
                    mode: auto
                  allow_mutations: false
                metrics:
                  enabled: false
                storage:
                  path: "data/calls.db"
                  cc_cache_file: "data/cc-cache.json"
                recordings:
                  dir: "recordings"
                  sample_rate: 8000
                  write_raw: false
                  skip_encrypted: true
                  equalizer:
                    enabled: %s
                    taps: 8
                    step_size: 0.0001
                retention:
                  call_log_days: 30
                  log_days: 30
                  files_days: 14
                  interval: "1h"
                sdr:
                  sample_rate: %d
                  watchdog_interval_ms: 30000
                  autotune: true
                  devices:
                """.formatted(system.modulation() == P25SystemConfig.Modulation.LSM_SIMULCAST, sampleRate));
        for (var device : devices) {
            boolean primary = device == control;
            out.append("    - serial: ").append(quote(device.serial().isBlank() ? device.id() : device.serial())).append('\n');
            out.append("      role: ").append(primary ? "wideband" : "voice").append('\n');
            out.append("      gain: \"auto\"\n      bias_tee: false\n");
            if (primary) {
                out.append("      center_freq_hz: ").append(center).append('\n');
                out.append("      tuner_strategy: auto\n      voice_taps: ").append(voiceTaps).append('\n');
                out.append("      channels:\n");
                for (long frequency : system.controlFrequenciesHz()) {
                    out.append("        - frequency_hz: ").append(frequency).append('\n');
                    out.append("          system: ").append(quote(id)).append('\n');
                }
            }
        }
        out.append("trunking:\n  call_timeout_ms: 30000\n  voice_hangtime_ms: 3500\n  voice_call_grouping: \"transmission\"\n  systems:\n");
        out.append("    - name: ").append(quote(id)).append("\n      protocol: p25\n      control_channels:\n");
        for (long frequency : system.controlFrequenciesHz()) out.append("        - ").append(frequency).append('\n');
        out.append("      p25_phase1_demod_mode: ")
                .append(system.modulation() == P25SystemConfig.Modulation.LSM_SIMULCAST ? "\"cqpsk\"" : "\"c4fm\"").append('\n');
        out.append("      encrypted_calls:\n        mode: ignore\n");
        return out.toString();
    }

    private static String slug(String value) {
        var slug = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "p25-system" : slug;
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", " ").replace("\n", " ") + "\"";
    }
}
