package app.sdrpole.desktop;

import app.sdrpole.core.AuditLog;
import app.sdrpole.core.DemodulationMode;
import app.sdrpole.core.FrequencyBand;
import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.HackRfSweepService;
import app.sdrpole.core.ScanPlan;
import app.sdrpole.core.ScanRange;
import app.sdrpole.core.ScanSpeed;
import app.sdrpole.core.SdrDevice;
import app.sdrpole.core.SignalObservation;
import app.sdrpole.core.directory.LocalSurveyRecorder;
import app.sdrpole.core.p25.P25SystemConfig;
import app.sdrpole.core.p25.P25SystemStore;
import javafx.application.Platform;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/** Coordinates zero-import discovery and native hardware sweeps without putting process/persistence logic in panes. */
final class ScanAutomationController {
    private final Preferences preferences;
    private final Supplier<List<SdrDevice>> devices;
    private final Supplier<Optional<GeoPoint>> location;
    private final List<P25SystemConfig> systems;
    private final P25SystemStore systemStore;
    private final LocalSurveyRecorder survey;
    private final AuditLog audit;
    private final Runnable stopReceiver;
    private final Runnable startP25;
    private final Consumer<String> status;
    private final Consumer<String> navigate;
    private final Consumer<GeoPoint> refreshChannels;
    private final HackRfSweepService hackRfSweep = new HackRfSweepService();

    ScanAutomationController(Preferences preferences, Supplier<List<SdrDevice>> devices,
                             Supplier<Optional<GeoPoint>> location, List<P25SystemConfig> systems,
                             P25SystemStore systemStore, LocalSurveyRecorder survey, AuditLog audit,
                             Runnable stopReceiver, Runnable startP25, Consumer<String> status,
                             Consumer<String> navigate, Consumer<GeoPoint> refreshChannels) {
        this.preferences = preferences; this.devices = devices; this.location = location; this.systems = systems;
        this.systemStore = systemStore; this.survey = survey; this.audit = audit; this.stopReceiver = stopReceiver;
        this.startP25 = startP25; this.status = status; this.navigate = navigate; this.refreshChannels = refreshChannels;
    }

    void discoverP25() {
        if (devices.get().isEmpty()) { status.accept("Connect an SDR first"); return; }
        var ranges = List.of(
                new ScanRange(769_000_000, 775_000_000, 12_500, DemodulationMode.NFM),
                new ScanRange(851_000_000, 869_000_000, 12_500, DemodulationMode.NFM),
                new ScanRange(150_000_000, 174_000_000, 12_500, DemodulationMode.NFM),
                new ScanRange(450_000_000, 470_000_000, 12_500, DemodulationMode.NFM));
        if (devices.get().stream().anyMatch(device -> "hackrf".equalsIgnoreCase(device.driver()))) {
            discoverP25WithHackRf(ranges); return;
        }
        preferences.put("scanner.ranges", ScanPlan.encode(ranges)); preferences.putBoolean("scanner.auto", true);
        preferences.putBoolean("scanner.trunkingDiscovery", true); preferences.put("scanner.speed", ScanSpeed.TURBO.name());
        preferences.put("lastFrequencyMhz", "150.00000"); preferences.put("lastMode", DemodulationMode.NFM.name());
        audit.record("trunking", "zero-import discovery started", "success", Map.of("rangeCount", ranges.size()));
        status.accept("Discovering P25 candidates across VHF, UHF, 700, and 800 MHz public-safety bands"); navigate.accept("Spectrum");
    }

    private void discoverP25WithHackRf(List<ScanRange> ranges) {
        stopReceiver.run(); status.accept("Hardware-sweeping P25 bands; no directory or import required…");
        Thread.startVirtualThread(() -> {
            try {
                var peaks = new java.util.ArrayList<HackRfSweepService.Peak>();
                for (var range : ranges) peaks.addAll(hackRfSweep.sweep(range.startHz(), range.endHz(), 12_500, 12));
                var candidates = peaks.stream().sorted(java.util.Comparator.comparingDouble(HackRfSweepService.Peak::powerDb).reversed())
                        .limit(32).toList();
                if (candidates.isEmpty()) { Platform.runLater(() -> status.accept("No strong P25-band candidates found; check antenna and gain")); return; }
                survey.recordSweep(candidates, location.get().orElse(null));
                Platform.runLater(() -> startP25Candidates(candidates));
            } catch (Exception problem) { Platform.runLater(() -> status.accept(problem.getMessage())); }
        });
    }

    private void startP25Candidates(List<HackRfSweepService.Peak> candidates) {
        try {
            var frequencies = candidates.stream().map(HackRfSweepService.Peak::frequencyHz).distinct().toList();
            var system = new P25SystemConfig("Locally discovered P25 candidates", "Automatic hardware sweep", frequencies,
                    P25SystemConfig.Modulation.C4FM, 4, location.get().orElse(null));
            systems.removeIf(item -> item.systemName().equals(system.systemName())); systems.add(system); systemStore.save(systems);
            audit.record("trunking", "hardware candidate sweep", "success", Map.of("candidateCount", frequencies.size()));
            status.accept("Testing " + frequencies.size() + " control-channel candidates with the local P25 engine…"); startP25.run();
        } catch (Exception problem) { status.accept("Candidates found, but P25 validation could not start: " + problem.getMessage()); }
    }

    void listen(FrequencyBand band) {
        preferences.put("lastFrequencyMhz", String.format("%.5f", band.startHz() / 1e6));
        preferences.put("lastMode", band.mode().name()); preferences.put("scanner.ranges", "");
        preferences.putBoolean("scanner.auto", false); navigate.accept("Spectrum");
    }

    void scan(List<FrequencyBand> bands) {
        var first = bands.getFirst();
        preferences.put("lastFrequencyMhz", String.format("%.5f", first.startHz() / 1e6));
        preferences.put("lastMode", first.mode().name());
        preferences.put("scanner.ranges", ScanPlan.encode(bands.stream()
                .map(band -> new ScanRange(band.startHz(), band.endHz(), band.stepHz(), band.mode())).toList()));
        preferences.putBoolean("scanner.auto", true);
        audit.record("scanner", "scan plan loaded", "success", Map.of("rangeCount", bands.size(), "firstRange", first.name()));
        navigate.accept("Spectrum");
    }

    boolean validateP25Candidate(SignalObservation signal) {
        if (!preferences.getBoolean("scanner.trunkingDiscovery", false)) return false;
        preferences.putBoolean("scanner.trunkingDiscovery", false);
        try {
            var candidate = new P25SystemConfig("Auto-discovered P25 candidate",
                    String.format("%.5f MHz", signal.frequencyHz() / 1e6), List.of(signal.frequencyHz()),
                    P25SystemConfig.Modulation.C4FM, 4, location.get().orElse(null));
            systems.removeIf(item -> item.systemName().equals(candidate.systemName())); systems.add(candidate); systemStore.save(systems);
            stopReceiver.run(); status.accept("Testing the discovered control-channel candidate with the local P25 engine…"); startP25.run();
        } catch (Exception problem) { status.accept("P25 candidate found, but validation could not start: " + problem.getMessage()); }
        return true;
    }

    void hardwareSweep(FrequencyBand range) {
        if (devices.get().stream().noneMatch(device -> "hackrf".equalsIgnoreCase(device.driver()))) {
            status.accept("Hardware sweep needs a connected HackRF; use normal Fast scan for other radios"); return;
        }
        stopReceiver.run(); status.accept("HackRF hardware sweep running with RF amp and antenna power safely off…");
        Thread.startVirtualThread(() -> {
            try {
                var peaks = hackRfSweep.sweep(range.startHz(), range.endHz(), (int) Math.min(100_000, range.stepHz()), 12);
                survey.recordSweep(peaks, location.get().orElse(null));
                Platform.runLater(() -> {
                    refreshChannels.accept(location.get().orElse(new GeoPoint(39.5, -98.35)));
                    status.accept("Hardware sweep found " + peaks.size() + " strong signal(s)"); navigate.accept("Scanner");
                });
            } catch (Exception problem) { Platform.runLater(() -> status.accept(problem.getMessage())); }
        });
    }
}
