package app.sdrpole.desktop;

import app.sdrpole.core.BuiltInDecoderCatalog;
import app.sdrpole.core.DecoderCatalogEntry;
import app.sdrpole.core.DeviceDiscoveryService;
import app.sdrpole.core.SdrDevice;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.List;

public final class SdrPoleApplication extends Application {
    private static final String BG = "#0b1118";
    private static final String PANEL = "#14202a";
    private static final String TEXT = "#edf7fa";
    private static final String MUTED = "#92a8b5";
    private static final String ACCENT = "#22c6da";

    private final DeviceDiscoveryService discovery = new DeviceDiscoveryService();
    private final BorderPane shell = new BorderPane();
    private final Label status = muted("Ready — connect one or more SDRs and choose Devices");
    private List<SdrDevice> devices = List.of();

    @Override public void start(Stage stage) {
        shell.setStyle("-fx-background-color: " + BG + ";");
        shell.setTop(topBar());
        shell.setLeft(navigation());
        shell.setCenter(home());
        shell.setBottom(statusBar());

        var scene = new Scene(shell, 1180, 760);
        stage.setTitle("SDR-Pole");
        stage.setMinWidth(980);
        stage.setMinHeight(650);
        stage.setScene(scene);
        stage.show();
        refreshDevices(false);
    }

    private Node topBar() {
        var title = label("SDR-Pole", 27, true);
        var badge = new Label("EARLY ACCESS");
        badge.setStyle("-fx-background-color: #163f49; -fx-text-fill: " + ACCENT +
                "; -fx-padding: 4 8; -fx-background-radius: 8; -fx-font-weight: bold;");
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var easy = muted("Simple mode");
        var advanced = new ToggleButton("Advanced");
        advanced.setTooltip(new Tooltip("Show tuner, gain, sample-rate, and DSP controls"));
        var bar = new HBox(12, title, badge, spacer, easy, advanced);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(18, 24, 14, 24));
        bar.setStyle("-fx-border-color: transparent transparent #263540 transparent;");
        return bar;
    }

    private Node navigation() {
        var items = FXCollections.observableArrayList(
                "Home", "Devices", "Systems", "Live Calls", "Spectrum", "Decoders",
                "Recordings", "Map", "Diagnostics", "Settings");
        var nav = new ListView<>(items);
        nav.setPrefWidth(180);
        nav.getSelectionModel().selectFirst();
        nav.setStyle("-fx-background-color: #0f1820; -fx-control-inner-background: #0f1820; " +
                "-fx-font-size: 14px; -fx-border-color: transparent #263540 transparent transparent;");
        nav.getSelectionModel().selectedItemProperty().addListener((o, old, selected) -> show(selected));
        return nav;
    }

    private Node statusBar() {
        var bar = new HBox(status);
        bar.setPadding(new Insets(9, 18, 9, 18));
        bar.setStyle("-fx-background-color: #0f1820; -fx-border-color: #263540 transparent transparent transparent;");
        return bar;
    }

    private void show(String page) {
        if (page == null) return;
        shell.setCenter(switch (page) {
            case "Home" -> home();
            case "Devices" -> devicesPage();
            case "Decoders" -> decodersPage();
            case "Systems" -> featurePage("Systems", "Import SDRTrunk playlists or add a local system",
                    "Favorites", "Automatic control-channel discovery", "Talkgroup aliases", "Portable backups");
            case "Live Calls" -> featurePage("Live Calls", "Every active call, understandable at a glance",
                    "Hold a talkgroup", "Priority scanning", "Per-call volume", "Encrypted-call indicator");
            case "Spectrum" -> featurePage("Spectrum & Waterfall", "See signal activity from every connected tuner",
                    "Multi-receiver views", "Click to tune", "Signal labels", "Automatic gain");
            case "Recordings" -> featurePage("Recordings", "Search calls by system, talkgroup, radio, or time",
                    "Automatic naming", "Retention rules", "Export audio", "Call metadata");
            case "Map" -> featurePage("Map", "Sites and radio locations without spreadsheet archaeology",
                    "Site coverage", "Neighbor sites", "GPS metadata", "Offline maps");
            case "Diagnostics" -> diagnosticsPage();
            default -> featurePage("Settings", "Sensible defaults first; advanced controls when needed",
                    "Audio output", "Storage", "Updates", "Privacy");
        });
    }

    private Node home() {
        var title = label("Listen in three steps", 30, true);
        var subtitle = muted("SDR-Pole finds the hardware, recommends settings, and explains anything that needs attention.");
        var cards = new HBox(14,
                card("1", "Connect radios", devices.isEmpty() ? "Looking for SDR devices…" : devices.size() + " device(s) ready"),
                card("2", "Choose systems", "Import or search your area"),
                card("3", "Press Listen", "Preflight checks audio and decoders"));
        for (var child : cards.getChildren()) HBox.setHgrow(child, Priority.ALWAYS);

        var start = primaryButton("Start listening");
        start.setOnAction(e -> preflightDialog());
        var discover = secondaryButton("Find my radios");
        discover.setOnAction(e -> { shell.setCenter(devicesPage()); refreshDevices(true); });
        var actions = new HBox(10, start, discover);

        var callTitle = label("What SDR-Pole is being built to handle", 19, true);
        var capabilities = new FlowPane(10, 10,
                pill("P25 Phase 1 & 2"), pill("Trunk following"), pill("Multiple SDRs"),
                pill("Analog NFM"), pill("DMR & NXDN"), pill("Recording"),
                pill("Talkgroup aliases"), pill("Spectrum & waterfall"), pill("Windows • macOS • Linux"));

        return page(new VBox(18, title, subtitle, cards, actions, new Separator(), callTitle, capabilities));
    }

    private Node devicesPage() {
        var title = label("Radio devices", 28, true);
        var subtitle = muted("Use several tuners at once. SDR-Pole will automatically assign systems across their bandwidth.");
        var list = new VBox(10);
        if (devices.isEmpty()) list.getChildren().add(emptyState("No radios found yet", "Connect an SDR, then refresh."));
        else devices.forEach(d -> list.getChildren().add(deviceRow(d)));
        var refresh = primaryButton("Refresh devices");
        refresh.setOnAction(e -> refreshDevices(true));
        var supported = muted("Driver architecture: HackRF • RTL-SDR • Airspy • SDRplay • LimeSDR • PlutoSDR • USRP • SoapyRemote");
        return page(new VBox(16, title, subtitle, refresh, list, supported));
    }

    private Node deviceRow(SdrDevice device) {
        var state = new Label(device.available() ? "READY" : "BUSY");
        state.setStyle("-fx-text-fill: " + (device.available() ? "#63e6a5" : "#ffb86b") + "; -fx-font-weight: bold;");
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var configure = secondaryButton("Configure");
        var row = new HBox(14, label(device.label(), 17, true), muted(device.driver() + "  •  " + shortSerial(device.serial())), spacer, state, configure);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));
        row.setStyle(panelStyle());
        return row;
    }

    private Node decodersPage() {
        var title = label("Decoder library", 28, true);
        var subtitle = muted("Install only what you need. SDR-Pole verifies compatibility, checksum, license, and rollback before activation.");
        var rows = new VBox(10);
        for (var decoder : BuiltInDecoderCatalog.entries()) rows.getChildren().add(decoderRow(decoder));
        var note = muted("Encrypted radio traffic is identified but is never decrypted. Decoder availability varies by platform and license.");
        return page(new VBox(16, title, subtitle, rows, note));
    }

    private Node decoderRow(DecoderCatalogEntry decoder) {
        var name = label(decoder.name(), 17, true);
        var description = muted(decoder.summary());
        description.setWrapText(true);
        var text = new VBox(4, name, description, muted("License: " + decoder.license()));
        HBox.setHgrow(text, Priority.ALWAYS);
        var button = switch (decoder.availability()) {
            case BUILT_IN -> secondaryButton("Installed");
            case READY_TO_INSTALL -> primaryButton("Install");
            case REQUIRES_PACKAGE -> secondaryButton("Package required");
            case COMING_SOON -> secondaryButton("Coming soon");
        };
        button.setDisable(decoder.availability() != DecoderCatalogEntry.Availability.READY_TO_INSTALL);
        var row = new HBox(14, text, button);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));
        row.setStyle(panelStyle());
        return row;
    }

    private Node diagnosticsPage() {
        var title = label("Diagnostics", 28, true);
        var checks = new VBox(10,
                check("Application", true, "Java runtime bundled"),
                check("SDR provider", !devices.isEmpty(), devices.isEmpty() ? "No device detected" : devices.size() + " radio(s) detected"),
                check("Audio output", true, "System default output available"),
                check("P25 decoder", false, "Decoder package has not been installed"));
        var copy = secondaryButton("Copy support report");
        return page(new VBox(16, title, muted("Plain-language checks with repair actions—no wall of USB errors."), checks, copy));
    }

    private Node featurePage(String titleText, String subtitleText, String... features) {
        var grid = new FlowPane(12, 12);
        for (var feature : features) grid.getChildren().add(card("✓", feature, "Designed for the public release roadmap"));
        return page(new VBox(16, label(titleText, 28, true), muted(subtitleText), grid));
    }

    private void refreshDevices(boolean announce) {
        status.setText("Searching USB and network SDR devices…");
        Thread.ofVirtual().start(() -> {
            var found = discovery.discover();
            Platform.runLater(() -> {
                devices = found;
                status.setText(found.isEmpty() ? "No SDR detected — reconnect it and open Diagnostics" :
                        found.size() + " SDR device(s) ready");
                if (announce) shell.setCenter(devicesPage());
            });
        });
    }

    private void preflightDialog() {
        var alert = new Alert(devices.isEmpty() ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION);
        alert.setTitle("Listening preflight");
        alert.setHeaderText(devices.isEmpty() ? "Connect an SDR first" : "Hardware is ready; receiver engine is next");
        alert.setContentText(devices.isEmpty()
                ? "SDR-Pole could not find a radio. Reconnect it, close other SDR applications, and choose Refresh devices."
                : "Detected " + devices.size() + " SDR device(s). Live IQ streaming and P25 audio are not enabled in this early build yet.");
        alert.showAndWait();
    }

    private VBox card(String step, String value, String detail) {
        var box = new VBox(8, accent(step), label(value, 17, true), muted(detail));
        box.setPadding(new Insets(18));
        box.setMinWidth(240);
        box.setStyle(panelStyle());
        return box;
    }

    private Node emptyState(String title, String detail) {
        var box = new VBox(5, label(title, 17, true), muted(detail));
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));
        box.setStyle(panelStyle());
        return box;
    }

    private Node check(String name, boolean okay, String detail) {
        var icon = accent(okay ? "✓" : "!");
        var row = new HBox(12, icon, label(name, 16, true), muted(detail));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14));
        row.setStyle(panelStyle());
        return row;
    }

    private ScrollPane page(Node content) {
        var holder = new VBox(content);
        holder.setPadding(new Insets(28));
        holder.setStyle("-fx-background-color: " + BG + ";");
        var scroll = new ScrollPane(holder);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + BG + "; -fx-background-color: " + BG + ";");
        return scroll;
    }

    private Label label(String text, int size, boolean bold) {
        var label = new Label(text);
        label.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: " + size + "px;" + (bold ? " -fx-font-weight: bold;" : ""));
        return label;
    }

    private static Label muted(String text) {
        var label = new Label(text);
        label.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
        return label;
    }

    private Label accent(String text) {
        var label = new Label(text);
        label.setStyle("-fx-text-fill: " + ACCENT + "; -fx-font-weight: bold; -fx-font-size: 14px;");
        return label;
    }

    private Label pill(String text) {
        var label = label(text, 13, true);
        label.setStyle(label.getStyle() + " -fx-background-color: #17313a; -fx-padding: 8 12; -fx-background-radius: 16;");
        return label;
    }

    private Button primaryButton(String text) {
        var button = new Button(text);
        button.setStyle("-fx-background-color: " + ACCENT + "; -fx-text-fill: #061418; -fx-font-weight: bold; -fx-padding: 10 16; -fx-background-radius: 8;");
        return button;
    }

    private Button secondaryButton(String text) {
        var button = new Button(text);
        button.setStyle("-fx-background-color: #20313d; -fx-text-fill: " + TEXT + "; -fx-padding: 9 14; -fx-background-radius: 8;");
        return button;
    }

    private static String panelStyle() {
        return "-fx-background-color: " + PANEL + "; -fx-background-radius: 12; -fx-border-color: #263b48; -fx-border-radius: 12;";
    }

    private static String shortSerial(String serial) {
        if (serial == null || serial.isBlank()) return "serial unavailable";
        return serial.length() <= 12 ? serial : "…" + serial.substring(serial.length() - 12);
    }

    public static void main(String[] args) { launch(args); }
}
