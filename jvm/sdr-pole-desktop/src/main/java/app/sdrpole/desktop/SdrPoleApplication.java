package app.sdrpole.desktop;

import app.sdrpole.core.BuiltInDecoderCatalog;
import app.sdrpole.core.DecoderCatalogEntry;
import app.sdrpole.core.DeviceDiscoveryService;
import app.sdrpole.core.DecoderLibraryManager;
import app.sdrpole.core.DemodulationMode;
import app.sdrpole.core.LiveNfmReceiver;
import app.sdrpole.core.JmbeInstaller;
import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.FrequencyPreset;
import app.sdrpole.core.ReceiverConfig;
import app.sdrpole.core.ReceiverListener;
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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.util.List;
import java.util.prefs.Preferences;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public final class SdrPoleApplication extends Application {
    private static final String BG = "#0b1118";
    private static final String PANEL = "#14202a";
    private static final String TEXT = "#edf7fa";
    private static final String MUTED = "#92a8b5";
    private static final String ACCENT = "#22c6da";

    private final DeviceDiscoveryService discovery = new DeviceDiscoveryService();
    private final DecoderLibraryManager libraries = new DecoderLibraryManager();
    private final Preferences preferences = Preferences.userNodeForPackage(SdrPoleApplication.class);
    private final BorderPane shell = new BorderPane();
    private final Label status = muted("Ready — connect one or more SDRs and choose Devices");
    private List<SdrDevice> devices = List.of();
    private LiveNfmReceiver activeReceiver;
    private ListView<String> navigation;
    private String currentPage = "Home";
    private boolean advancedMode;
    private SdrDevice preferredDevice;

    @Override public void start(Stage stage) {
        advancedMode = preferences.getBoolean("advancedMode", false);
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
        var easy = muted(advancedMode ? "Lab mode" : "Simple mode");
        var advanced = new ToggleButton("Advanced");
        advanced.setSelected(advancedMode);
        advanced.setTooltip(new Tooltip("Show tuner, gain, sample-rate, and DSP controls"));
        advanced.setOnAction(event -> {
            advancedMode = advanced.isSelected();
            preferences.putBoolean("advancedMode", advancedMode);
            easy.setText(advancedMode ? "Lab mode" : "Simple mode");
            show(currentPage);
        });
        var bar = new HBox(12, title, badge, spacer, easy, advanced);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(18, 24, 14, 24));
        bar.setStyle("-fx-border-color: transparent transparent #263540 transparent;");
        return bar;
    }

    private Node navigation() {
        var items = FXCollections.observableArrayList(
                "Home", "Devices", "Nearby", "Systems", "Live Calls", "Spectrum", "Decoders",
                "Recordings", "Map", "Diagnostics", "Settings");
        navigation = new ListView<>(items);
        var nav = navigation;
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
        currentPage = page;
        shell.setCenter(switch (page) {
            case "Home" -> home();
            case "Devices" -> devicesPage();
            case "Nearby" -> nearbyPage();
            case "Decoders" -> decodersPage();
            case "Systems" -> featurePage("Systems", "Import SDRTrunk playlists or add a local system",
                    "Favorites", "Automatic control-channel discovery", "Talkgroup aliases", "Portable backups");
            case "Live Calls" -> featurePage("Live Calls", "Every active call, understandable at a glance",
                    "Hold a talkgroup", "Priority scanning", "Per-call volume", "Encrypted-call indicator");
            case "Spectrum" -> spectrumPage();
            case "Recordings" -> featurePage("Recordings", "Search calls by system, talkgroup, radio, or time",
                    "Automatic naming", "Retention rules", "Export audio", "Call metadata");
            case "Map" -> featurePage("Map & direction finding", "Known sites, observations, and bearings—without pretending one receiver can locate a transmitter",
                    "Licensed-site map", "Signal observations", "Bearing overlays", "Coherent-array roadmap");
            case "Diagnostics" -> diagnosticsPage();
            default -> featurePage("Settings", "Sensible defaults first; advanced controls when needed",
                    "Audio output", "Storage", "Updates", "Privacy");
        });
    }

    private Node home() {
        var title = label("What would you like to hear?", 30, true);
        var subtitle = muted("Choose a goal. SDR-Pole will fill in safe defaults and only show the controls you need.");

        var quick = actionCard("Explore a frequency", "Hear analog AM, FM, sideband, or CW with a live waterfall.",
                devices.isEmpty() ? "Connect a radio first" : "Ready with " + friendlyDeviceName(devices.getFirst()), "Open quick tuner", () -> {
                    preferredDevice = devices.isEmpty() ? null : devices.getFirst();
                    navigateTo("Spectrum");
                });
        ((Button) quick.getChildren().getLast()).setDisable(devices.isEmpty());
        var trunked = actionCard("Listen to a trunked system", "Guided site, control-channel, talkgroup, and decoder setup.",
                "P25 frame decoder is still under construction", "View readiness", this::preflightDialog);
        HBox.setHgrow(quick, Priority.ALWAYS);
        HBox.setHgrow(trunked, Priority.ALWAYS);

        var readiness = new VBox(8,
                readinessRow("Radio connected", !devices.isEmpty(), devices.isEmpty() ? discovery.lastDiagnostic() : devices.size() + " detected"),
                readinessRow("Audio output", hasAudioOutput(), hasAudioOutput() ? "System output is available" : "No compatible output line found"),
                readinessRow("Analog listening", !devices.isEmpty(), devices.isEmpty() ? "Waiting for a radio" : "Ready now"),
                readinessRow("JMBE voice library", libraries.jmbePath().isPresent(), libraries.jmbePath().isPresent() ? "Installed" : "Optional—install from Decoders"),
                readinessRow("P25 trunking engine", false, "Not operational yet; no false Ready label"));
        var refresh = secondaryButton(devices.isEmpty() ? "Find my radio" : "Refresh radios");
        refresh.setOnAction(event -> refreshDevices(false));

        return page(new VBox(18, title, subtitle, new HBox(14, quick, trunked),
                new Separator(), label("Readiness", 19, true), readiness, refresh));
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

    private Node nearbyPage() {
        var title = label("Find signals near me", 28, true);
        var subtitle = muted("Start with a location, combine trustworthy directories with live RF observations, then tune a result in one click.");
        var latitude = new TextField(preferences.get("location.latitude", ""));
        latitude.setPromptText("Latitude");
        latitude.setPrefWidth(120);
        var longitude = new TextField(preferences.get("location.longitude", ""));
        longitude.setPromptText("Longitude");
        longitude.setPrefWidth(120);
        var radius = new Spinner<Integer>(1, 500, 50, 5);
        radius.setPrefWidth(95);
        var locationState = muted("Enter coordinates or use a future GPS provider.");
        var apply = primaryButton("Use this location");
        apply.setOnAction(event -> {
            try {
                var point = new GeoPoint(Double.parseDouble(latitude.getText().trim()), Double.parseDouble(longitude.getText().trim()));
                preferences.put("location.latitude", latitude.getText().trim());
                preferences.put("location.longitude", longitude.getText().trim());
                locationState.setText(String.format("Location ready: %.4f, %.4f • %d km search radius", point.latitude(), point.longitude(), radius.getValue()));
                status.setText("Local search location updated");
            } catch (RuntimeException error) {
                locationState.setText("Enter valid latitude (-90…90) and longitude (-180…180)");
            }
        });
        var location = new HBox(10, field("Latitude", latitude), field("Longitude", longitude),
                field("Radius (km)", radius), apply);
        location.setAlignment(Pos.BOTTOM_LEFT);

        var sources = new VBox(10,
                sourceRow("FCC ULS open data", "Public U.S. license and site records. Useful evidence, but not a real-time list of active signals.", "Planned importer"),
                sourceRow("RadioReference", "Optional authenticated directory. Each user supplies their own eligible account and API access.", "Connection planned"),
                sourceRow("Live spectrum survey", "Measure energy with your SDR, group repeated signals, and compare observations over time.", "Scanner foundation"),
                sourceRow("Files and playlists", "Import SDRTrunk playlists, CSV frequency lists, and SigMF recordings without retyping them.", "Import roadmap"));
        var truth = muted("Location truth matters: a single HackRF can show frequency and signal strength, but cannot determine a transmitter position. Direction finding needs bearings from multiple locations or coherent multi-channel hardware such as KrakenSDR.");
        truth.setWrapText(true);
        return page(new VBox(16, title, subtitle, location, locationState, new Separator(),
                label("Discovery sources", 19, true), sources, truth));
    }

    private Node sourceRow(String name, String description, String state) {
        var copy = new VBox(4, label(name, 17, true), muted(description));
        ((Label) copy.getChildren().get(1)).setWrapText(true);
        HBox.setHgrow(copy, Priority.ALWAYS);
        var badge = accent(state);
        var row = new HBox(14, copy, badge);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));
        row.setStyle(panelStyle());
        return row;
    }

    private Node deviceRow(SdrDevice device) {
        var state = new Label(device.available() ? "READY" : "BUSY");
        state.setStyle("-fx-text-fill: " + (device.available() ? "#63e6a5" : "#ffb86b") + "; -fx-font-weight: bold;");
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var configure = secondaryButton("Configure");
        configure.setOnAction(event -> {
            preferredDevice = device;
            navigateTo("Spectrum");
        });
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

    private Node spectrumPage() {
        var title = label(advancedMode ? "Spectrum lab" : "Quick tuner", 28, true);
        var subtitle = muted(advancedMode
                ? "Inspect the receiver and control its sample rate, gain, mode, and center frequency."
                : "Choose an example or enter a frequency, then press Listen. Recommended radio settings are automatic.");
        var deviceChoice = new ComboBox<SdrDevice>();
        deviceChoice.getItems().setAll(devices);
        deviceChoice.setPromptText(devices.isEmpty() ? "No SDR detected" : "Choose a radio");
        deviceChoice.setPrefWidth(250);
        deviceChoice.setButtonCell(deviceCell());
        deviceChoice.setCellFactory(list -> deviceCell());
        if (preferredDevice != null && devices.contains(preferredDevice)) deviceChoice.getSelectionModel().select(preferredDevice);
        else if (!devices.isEmpty()) deviceChoice.getSelectionModel().selectFirst();

        var frequency = new TextField(preferences.get("lastFrequencyMhz", "162.55000"));
        frequency.setPromptText("Frequency in MHz");
        frequency.setPrefWidth(130);
        var sampleRate = new ComboBox<Integer>();
        sampleRate.getItems().addAll(2_000_000, 4_000_000, 8_000_000, 10_000_000);
        sampleRate.getSelectionModel().selectFirst();
        sampleRate.setButtonCell(rateCell());
        sampleRate.setCellFactory(list -> rateCell());
        var mode = new ComboBox<DemodulationMode>();
        mode.getItems().addAll(DemodulationMode.values());
        try { mode.getSelectionModel().select(DemodulationMode.valueOf(preferences.get("lastMode", "NFM"))); }
        catch (IllegalArgumentException ignored) { mode.getSelectionModel().select(DemodulationMode.NFM); }
        mode.setPrefWidth(150);

        var preset = new ComboBox<FrequencyPreset>();
        preset.getItems().addAll(FrequencyPreset.commonNorthAmerica());
        preset.setPromptText("Choose a common example…");
        preset.setPrefWidth(270);
        preset.valueProperty().addListener((observable, old, selected) -> {
            if (selected == null) return;
            frequency.setText(String.format("%.5f", selected.frequencyMhz()));
            mode.getSelectionModel().select(selected.mode());
        });

        var automaticGain = new CheckBox("Automatic gain");
        automaticGain.setStyle("-fx-text-fill: " + TEXT + ";");
        var gain = new Slider(0, 62, 24);
        gain.setPrefWidth(145);
        gain.disableProperty().bind(automaticGain.selectedProperty());
        var gainLabel = muted("Gain 24 dB");
        gain.valueProperty().addListener((o, old, value) -> gainLabel.setText("Gain " + Math.round(value.doubleValue()) + " dB"));

        var essentials = new HBox(10, field("Radio", deviceChoice), field("Frequency (MHz)", frequency), field("Mode", mode));
        essentials.setAlignment(Pos.BOTTOM_LEFT);
        var advancedControls = new HBox(10, field("Sample rate", sampleRate),
                field("Tuner gain", new VBox(2, gain, gainLabel)), automaticGain);
        advancedControls.setAlignment(Pos.BOTTOM_LEFT);
        var controls = new VBox(10, field("Quick examples", preset), essentials);
        if (advancedMode) controls.getChildren().add(advancedControls);
        else controls.getChildren().add(muted("Recommended settings: 2 MS/s • 24 dB gain • mono system audio"));

        var waterfall = new WaterfallView(900, 300);
        var receiverStatus = label("Stopped", 15, true);
        var level = new ProgressBar(0);
        level.setPrefWidth(180);
        var listen = primaryButton("Listen");
        var stop = secondaryButton("Stop");
        stop.setDisable(true);

        listen.setOnAction(event -> {
            var selected = deviceChoice.getValue();
            if (selected == null) {
                receiverStatus.setText("Connect and select an SDR first");
                return;
            }
            try {
                long hz = Math.round(Double.parseDouble(frequency.getText().trim()) * 1_000_000);
                preferences.put("lastFrequencyMhz", frequency.getText().trim());
                preferences.put("lastMode", mode.getValue().name());
                closeReceiver();
                activeReceiver = new LiveNfmReceiver(
                        new ReceiverConfig(selected, hz, sampleRate.getValue(), gain.getValue(), automaticGain.isSelected(), 48_000, mode.getValue()),
                        new ReceiverListener() {
                            @Override public void onStatus(String message) { Platform.runLater(() -> receiverStatus.setText(message)); }
                            @Override public void onSpectrum(float[] powerDb) { waterfall.accept(powerDb); }
                            @Override public void onLevel(double rms) { Platform.runLater(() -> level.setProgress(Math.min(1, rms * 5))); }
                            @Override public void onError(String friendlyMessage, Throwable cause) {
                                Platform.runLater(() -> receiverStatus.setText(friendlyMessage));
                            }
                        });
                activeReceiver.start();
                listen.setDisable(true);
                stop.setDisable(false);
            } catch (NumberFormatException e) {
                receiverStatus.setText("Enter a frequency such as 155.25000");
            } catch (RuntimeException e) {
                receiverStatus.setText(e.getMessage());
            }
        });
        stop.setOnAction(event -> {
            closeReceiver();
            listen.setDisable(false);
            stop.setDisable(true);
            receiverStatus.setText("Stopped");
        });

        var transport = new HBox(10, listen, stop, receiverStatus, muted("Audio level"), level);
        transport.setAlignment(Pos.CENTER_LEFT);
        var help = muted(advancedMode
                ? "P25 requires a frame decoder plus a compatible voice package; JMBE alone is not a signal decoder."
                : "Not sure where to begin? Try a NOAA Weather example. Reception depends on your location, antenna, and local signals.");
        help.setWrapText(true);
        return page(new VBox(16, title, subtitle, controls, waterfall, transport, help));
    }

    private Node decoderRow(DecoderCatalogEntry decoder) {
        var name = label(decoder.name(), 17, true);
        var description = muted(decoder.summary());
        description.setWrapText(true);
        var text = new VBox(4, name, description, muted("License: " + decoder.license()));
        HBox.setHgrow(text, Priority.ALWAYS);
        if (decoder.id().equals("jmbe")) return jmbeRow(decoder, text);
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

    private Node jmbeRow(DecoderCatalogEntry decoder, VBox text) {
        var installed = libraries.jmbePath();
        if (installed.isPresent()) text.getChildren().add(accent("Installed: " + installed.get().getFileName()));
        var progress = new ProgressBar(0);
        progress.setPrefWidth(150);
        progress.setVisible(false);
        progress.setManaged(false);
        var download = primaryButton(installed.isPresent() ? "Reinstall" : "Install JMBE");
        download.setOnAction(event -> installJmbe(text, download, progress));
        var select = secondaryButton("Select JAR");
        select.setOnAction(event -> selectJmbe(text));
        var actions = new HBox(8, progress, download, select);
        actions.setAlignment(Pos.CENTER_RIGHT);
        var row = new HBox(14, text, actions);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));
        row.setStyle(panelStyle());
        return row;
    }

    private void installJmbe(VBox text, Button download, ProgressBar progress) {
        var notice = new Alert(Alert.AlertType.CONFIRMATION);
        notice.setTitle("Install JMBE voice library");
        notice.setHeaderText("Download, verify, build, and activate JMBE " + JmbeInstaller.VERSION + "?");
        notice.setContentText("JMBE is a separate GPL-3.0 project. Its authors warn that compiled IMBE/AMBE implementations may be covered by patents in some locations. You are responsible for determining whether use is lawful where you live. SDR-Pole will download the official platform-specific Creator, verify its pinned SHA-256 checksum, build the tagged source, validate the JAR, and select it immediately.\n\nJMBE converts supported voice frames; a P25/DMR frame decoder is still required.");
        var accepted = notice.showAndWait().filter(button -> button == ButtonType.OK).isPresent();
        if (!accepted) return;

        download.setDisable(true);
        progress.setProgress(0);
        progress.setVisible(true);
        progress.setManaged(true);
        status.setText("Preparing JMBE installation…");
        Thread.ofVirtual().start(() -> {
            try {
                var jar = new JmbeInstaller(libraries).install((message, amount) -> Platform.runLater(() -> {
                    progress.setProgress(amount);
                    status.setText(message);
                }));
                Platform.runLater(() -> {
                    text.getChildren().removeIf(node -> node instanceof Label label && label.getText().startsWith("Installed:"));
                    text.getChildren().add(accent("Installed: " + jar.getFileName()));
                    progress.setVisible(false);
                    progress.setManaged(false);
                    download.setText("Reinstall");
                    download.setDisable(false);
                    status.setText("JMBE " + JmbeInstaller.VERSION + " installed, validated, and activated");
                });
            } catch (Exception error) {
                Platform.runLater(() -> {
                    progress.setVisible(false);
                    progress.setManaged(false);
                    download.setDisable(false);
                    status.setText("JMBE installation failed: " + error.getMessage());
                });
            }
        });
    }

    private void selectJmbe(VBox text) {
        var chooser = new FileChooser();
        chooser.setTitle("Select a JMBE library");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java libraries", "*.jar"));
        var selected = chooser.showOpenDialog(shell.getScene().getWindow());
        if (selected == null) return;
        try {
            libraries.selectJmbe(selected.toPath());
            text.getChildren().removeIf(node -> node instanceof Label label && label.getText().startsWith("Installed:"));
            text.getChildren().add(accent("Installed: " + selected.getName()));
            status.setText("JMBE library validated and selected");
        } catch (Exception e) {
            status.setText(e.getMessage());
        }
    }

    private Node diagnosticsPage() {
        var title = label("Diagnostics", 28, true);
        var checks = new VBox(10,
                check("Application", true, "Java runtime bundled"),
                check("SDR provider", !devices.isEmpty(), devices.isEmpty() ? discovery.lastDiagnostic() : devices.size() + " radio(s) detected"),
                check("Audio output", hasAudioOutput(), hasAudioOutput() ? "Compatible system output available" : "No compatible 48 kHz output found"),
                check("JMBE voice library", libraries.jmbePath().isPresent(), libraries.jmbePath().isPresent() ? "Installed and validated" : "Optional package not installed"),
                check("P25 decoder", false, "Decoder package has not been installed"));
        var copy = secondaryButton("Copy support report");
        copy.setOnAction(event -> copySupportReport());
        return page(new VBox(16, title, muted("Plain-language checks with repair actions—no wall of USB errors."), checks, copy));
    }

    private void copySupportReport() {
        var report = """
                SDR-Pole support report
                OS: %s %s
                Java: %s
                Radios detected: %d
                Discovery: %s
                Audio output: %s
                JMBE: %s
                P25 frame decoder: not installed
                """.formatted(
                System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("java.version"),
                devices.size(), discovery.lastDiagnostic(), hasAudioOutput() ? "available" : "unavailable",
                libraries.jmbePath().isPresent() ? "installed" : "not installed");
        var content = new ClipboardContent();
        content.putString(report);
        Clipboard.getSystemClipboard().setContent(content);
        status.setText("Support report copied—radio serial numbers and location were omitted");
    }

    private static boolean hasAudioOutput() {
        var format = new AudioFormat(48_000, 16, 1, true, false);
        return AudioSystem.isLineSupported(new DataLine.Info(SourceDataLine.class, format));
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
                status.setText(found.isEmpty() ? discovery.lastDiagnostic() :
                        found.size() + " SDR device(s) ready");
                if (announce) shell.setCenter(devicesPage());
                else if ("Home".equals(currentPage)) shell.setCenter(home());
                else if ("Devices".equals(currentPage)) shell.setCenter(devicesPage());
            });
        });
    }

    private void preflightDialog() {
        var alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Trunked listening readiness");
        alert.setHeaderText("P25 trunking is not ready yet");
        alert.setContentText((devices.isEmpty()
                ? "✗ Radio: none detected\n"
                : "✓ Radio: " + devices.getFirst().label() + "\n")
                + (libraries.jmbePath().isPresent()
                ? "✓ JMBE voice library: installed\n"
                : "○ JMBE voice library: optional package not installed\n")
                + "✗ P25 frame and control-channel decoder: under construction\n\n"
                + "Analog listening is available now. SDR-Pole will not claim trunking is ready until the complete decoder path passes test recordings.");
        var analog = new ButtonType("Open analog tuner", ButtonBar.ButtonData.OK_DONE);
        var decoders = new ButtonType("Open decoders", ButtonBar.ButtonData.OTHER);
        alert.getButtonTypes().setAll(analog, decoders, ButtonType.CLOSE);
        alert.showAndWait().ifPresent(choice -> {
            if (choice == analog) navigateTo("Spectrum");
            else if (choice == decoders) navigateTo("Decoders");
        });
    }

    private void navigateTo(String page) {
        if (navigation == null) { show(page); return; }
        var selected = navigation.getSelectionModel().getSelectedItem();
        navigation.getSelectionModel().select(page);
        if (page.equals(selected)) show(page);
    }

    private VBox actionCard(String title, String detail, String state, String action, Runnable handler) {
        var description = muted(detail);
        description.setWrapText(true);
        var button = primaryButton(action);
        button.setOnAction(event -> handler.run());
        var box = new VBox(10, label(title, 20, true), description, accent(state), button);
        box.setPadding(new Insets(20));
        box.setMinWidth(330);
        box.setStyle(panelStyle());
        return box;
    }

    private Node readinessRow(String name, boolean ready, String detail) {
        var icon = label(ready ? "✓" : "○", 18, true);
        icon.setStyle(icon.getStyle() + " -fx-text-fill: " + (ready ? "#63e6a5" : "#ffb86b") + ";");
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var row = new HBox(10, icon, label(name, 15, true), spacer, muted(detail));
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12, 14, 12, 14));
        row.setStyle(panelStyle());
        return row;
    }

    private VBox card(String step, String value, String detail) {
        var box = new VBox(8, accent(step), label(value, 17, true), muted(detail));
        box.setPadding(new Insets(18));
        box.setMinWidth(240);
        box.setStyle(panelStyle());
        return box;
    }

    private VBox field(String title, Node control) {
        return new VBox(5, muted(title), control);
    }

    private ListCell<SdrDevice> deviceCell() {
        return new ListCell<>() {
            @Override protected void updateItem(SdrDevice item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label() + " (" + shortSerial(item.serial()) + ")");
            }
        };
    }

    private ListCell<Integer> rateCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : (item / 1_000_000) + " MS/s");
            }
        };
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

    private static String friendlyDeviceName(SdrDevice device) {
        var label = device.label();
        var numberedSuffix = label.indexOf(" #");
        return numberedSuffix > 0 ? label.substring(0, numberedSuffix) : label;
    }

    private void closeReceiver() {
        var receiver = activeReceiver;
        activeReceiver = null;
        if (receiver != null) receiver.close();
    }

    @Override public void stop() { closeReceiver(); }

    public static void main(String[] args) { launch(args); }
}
