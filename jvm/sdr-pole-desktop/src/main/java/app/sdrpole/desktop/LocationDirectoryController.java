package app.sdrpole.desktop;

import app.sdrpole.core.AuditLog;
import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.directory.RadioReferenceCredentials;
import app.sdrpole.core.directory.RadioReferenceDirectoryClient;
import app.sdrpole.core.p25.P25SystemConfig;
import app.sdrpole.core.p25.P25SystemStore;
import app.sdrpole.core.p25.P25Talkgroup;
import app.sdrpole.core.p25.P25TalkgroupStore;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/** Owns authenticated directory orchestration; provider parsing and persistence remain core services. */
final class LocationDirectoryController {
    private final Preferences preferences;
    private final List<P25SystemConfig> systems;
    private final List<P25Talkgroup> talkgroups;
    private final P25SystemStore systemStore;
    private final P25TalkgroupStore talkgroupStore;
    private final RadioReferenceDirectoryClient client;
    private final AuditLog audit;
    private final Supplier<Optional<GeoPoint>> location;
    private final Supplier<Window> owner;
    private final Consumer<String> status;
    private final Runnable refreshWorkstation;
    private String sessionPassword = "";

    LocationDirectoryController(Preferences preferences, List<P25SystemConfig> systems, List<P25Talkgroup> talkgroups,
                                P25SystemStore systemStore, P25TalkgroupStore talkgroupStore,
                                RadioReferenceDirectoryClient client, AuditLog audit,
                                Supplier<Optional<GeoPoint>> location, Supplier<Window> owner,
                                Consumer<String> status, Runnable refreshWorkstation) {
        this.preferences = preferences; this.systems = systems; this.talkgroups = talkgroups;
        this.systemStore = systemStore; this.talkgroupStore = talkgroupStore; this.client = client; this.audit = audit;
        this.location = location; this.owner = owner; this.status = status; this.refreshWorkstation = refreshWorkstation;
    }

    String summary() {
        long updated = preferences.getLong("directory.rr.updated", 0);
        if (updated == 0) return "Not connected yet.";
        var age = Duration.between(Instant.ofEpochMilli(updated), Instant.now());
        var area = preferences.get("directory.rr.area", "selected area");
        var count = preferences.getInt("directory.rr.talkgroups", 0);
        var suffix = count > 0 ? " • " + count + " named talkgroups." : ".";
        return age.toDays() == 0 ? "Updated today for " + area + suffix
                : "Updated " + age.toDays() + " day(s) ago for " + area + suffix;
    }

    void refreshIfNeeded(GeoPoint point) {
        if (!sessionPassword.isBlank() && refreshNeeded(point)) update(false);
    }

    void connect() {
        if (location.get().isEmpty()) {
            alert(Alert.AlertType.INFORMATION, "Choose an area", "Where do you want to listen?",
                    "Click your listening area on the map first. SDR-Pole uses that point to load the correct county and statewide systems.");
            return;
        }
        var dialog = new Dialog<RadioReferenceCredentials>();
        dialog.setTitle("Connect RadioReference"); dialog.setHeaderText("Optional trunking and talkgroup enrichment"); dialog.initOwner(owner.get());
        var connect = new ButtonType("Connect & update", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, connect);
        var appKey = new PasswordField();
        appKey.setText(System.getProperty("sdrpole.radioreference.appKey", preferences.get("directory.rr.appKey", "")));
        appKey.setPromptText("SDR-Pole application key");
        var username = new TextField(preferences.get("directory.rr.username", "")); username.setPromptText("RadioReference username");
        var password = new PasswordField(); password.setPromptText("RadioReference password");
        var explanation = new Label("RadioReference is optional. It requires an approved application key and each user's active Premium account. The password remains in memory for this session.");
        explanation.setWrapText(true); explanation.setStyle("-fx-text-fill:#4f6470;");
        var form = new VBox(9, explanation, field("Application key", appKey), field("Username", username), field("Password", password));
        form.setPrefWidth(520); dialog.getDialogPane().setContent(form);
        dialog.setResultConverter(button -> button == connect ? new RadioReferenceCredentials(appKey.getText(), username.getText(), password.getText()) : null);
        dialog.showAndWait().ifPresent(credentials -> {
            preferences.put("directory.rr.appKey", credentials.appKey()); preferences.put("directory.rr.username", credentials.username());
            sessionPassword = credentials.password(); update(true);
        });
    }

    private boolean refreshNeeded(GeoPoint point) {
        long updated = preferences.getLong("directory.rr.updated", 0);
        if (updated == 0 || Duration.between(Instant.ofEpochMilli(updated), Instant.now()).toHours() >= 24) return true;
        try {
            return point.distanceKmTo(new GeoPoint(preferences.getDouble("directory.rr.latitude", 999),
                    preferences.getDouble("directory.rr.longitude", 999))) >= 25;
        } catch (RuntimeException ignored) { return true; }
    }

    private void update(boolean showCompletion) {
        var point = location.get().orElse(null);
        if (point == null || sessionPassword.isBlank()) return;
        final RadioReferenceCredentials credentials;
        try { credentials = new RadioReferenceCredentials(preferences.get("directory.rr.appKey", ""),
                preferences.get("directory.rr.username", ""), sessionPassword); }
        catch (RuntimeException problem) { status.accept(problem.getMessage()); return; }
        status.accept("Updating verified P25 systems for your location…");
        Thread.startVirtualThread(() -> {
            try {
                var update = client.fetch(point, credentials);
                Platform.runLater(() -> apply(update, point, showCompletion));
            } catch (Exception problem) {
                Platform.runLater(() -> {
                    audit.record("directory", "RadioReference update", "failure", Map.of("errorType", problem.getClass().getSimpleName()));
                    status.accept("Directory update failed: " + problem.getMessage());
                    alert(Alert.AlertType.WARNING, "Local radio data could not update", "Check the account, network, or application key", problem.getMessage());
                });
            }
        });
    }

    private void apply(app.sdrpole.core.directory.DirectoryUpdate update, GeoPoint point, boolean showCompletion) {
        try {
            for (var site : update.p25Sites()) {
                systems.removeIf(existing -> existing.systemName().equalsIgnoreCase(site.systemName()) && existing.siteName().equalsIgnoreCase(site.siteName()));
                systems.add(site);
            }
            var changedSystems = update.talkgroups().stream().map(P25Talkgroup::systemName).map(String::toLowerCase)
                    .collect(java.util.stream.Collectors.toSet());
            talkgroups.removeIf(item -> changedSystems.contains(item.systemName().toLowerCase())); talkgroups.addAll(update.talkgroups());
            systemStore.save(systems); talkgroupStore.save(talkgroups);
            preferences.putLong("directory.rr.updated", update.retrievedAt().toEpochMilli()); preferences.put("directory.rr.area", update.areaLabel());
            preferences.putInt("directory.rr.talkgroups", update.talkgroups().size());
            preferences.putDouble("directory.rr.latitude", point.latitude()); preferences.putDouble("directory.rr.longitude", point.longitude());
            audit.record("directory", "RadioReference update", "success", Map.of("siteCount", update.p25Sites().size(),
                    "talkgroupCount", update.talkgroups().size(), "provider", update.provider()));
            status.accept("Loaded " + update.p25Sites().size() + " P25 site(s) and " + update.talkgroups().size() + " talkgroups for " + update.areaLabel());
            refreshWorkstation.run();
            if (showCompletion) alert(Alert.AlertType.INFORMATION, "Local radio data is ready", update.areaLabel(),
                    "Loaded " + update.p25Sites().size() + " P25 site(s) and " + update.talkgroups().size() + " named talkgroups. The offline cache is ready.");
        } catch (Exception saveError) { status.accept("Local radio data could not be saved: " + saveError.getMessage()); }
    }

    private void alert(Alert.AlertType type, String title, String header, String message) {
        var alert = new Alert(type, message, ButtonType.OK); alert.setTitle(title); alert.setHeaderText(header); alert.initOwner(owner.get()); alert.showAndWait();
    }

    private static VBox field(String name, javafx.scene.Node node) {
        var label = new Label(name); label.setStyle("-fx-text-fill:#4f6470;-fx-font-size:12px;"); return new VBox(3, label, node);
    }
}
