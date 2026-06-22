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
import app.sdrpole.core.FastScanPlan;
import app.sdrpole.core.ReceiverConfig;
import app.sdrpole.core.ReceiverListener;
import app.sdrpole.core.RfFrontendSettings;
import app.sdrpole.core.P25EngineManager;
import app.sdrpole.core.ScanPlan;
import app.sdrpole.core.ScanSpeed;
import app.sdrpole.core.AuditLog;
import app.sdrpole.core.p25.P25SystemConfig;
import app.sdrpole.core.p25.P25SystemStore;
import app.sdrpole.core.p25.P25RuntimeConfigurator;
import app.sdrpole.core.p25.P25RuntimeManager;
import app.sdrpole.core.p25.P25CsvImporter;
import app.sdrpole.core.p25.P25Talkgroup;
import app.sdrpole.core.p25.P25TalkgroupStore;
import app.sdrpole.core.directory.RadioReferenceDirectoryClient;
import app.sdrpole.core.directory.FrequencyChannel;
import app.sdrpole.core.directory.FrequencyDatabase;
import app.sdrpole.core.directory.LocalSurveyRecorder;
import app.sdrpole.core.SdrDevice;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.beans.property.SimpleBooleanProperty;
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
import java.util.ArrayList;
import java.util.prefs.Preferences;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
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
    private final P25SystemStore p25SystemStore = new P25SystemStore();
    private final P25TalkgroupStore p25TalkgroupStore = new P25TalkgroupStore();
    private final P25EngineManager p25Engine = new P25EngineManager();
    private final P25RuntimeConfigurator p25Configurator = new P25RuntimeConfigurator();
    private final P25RuntimeManager p25Runtime = new P25RuntimeManager();
    private final FrequencyDatabase frequencyDatabase = new FrequencyDatabase();
    private final LocalSurveyRecorder localSurvey = new LocalSurveyRecorder(frequencyDatabase);
    private final AuditLog audit = new AuditLog();
    private final BorderPane shell = new BorderPane();
    private final Label status = muted("Ready — connect one or more SDRs and choose Devices");
    private List<SdrDevice> devices = List.of();
    private LiveNfmReceiver activeReceiver;
    private ListView<String> navigation;
    private String currentPage = "Home";
    private boolean advancedMode;
    private SdrDevice preferredDevice;
    private final List<P25SystemConfig> configuredSystems = new ArrayList<>();
    private final List<P25Talkgroup> configuredTalkgroups = new ArrayList<>();
    private List<FrequencyChannel> localChannels = List.of();
    private SiteMapView siteMap;
    private LocationDirectoryController directoryController;
    private ScanAutomationController scanAutomation;

    @Override public void start(Stage stage) {
        audit.record("application", "start", "success", Map.of("version", "0.2.0", "offlineCapable", true));
        advancedMode = preferences.getBoolean("advancedMode", false);
        try { configuredSystems.addAll(p25SystemStore.load()); }
        catch (Exception error) { status.setText("Saved P25 sites could not be loaded: " + error.getMessage()); }
        try { configuredTalkgroups.addAll(p25TalkgroupStore.load()); }
        catch (Exception error) { status.setText("Saved P25 talkgroups could not be loaded: " + error.getMessage()); }
        try {
            frequencyDatabase.initialize();
            localChannels = frequencyDatabase.channelsNear(savedLocation().orElse(new GeoPoint(39.5, -98.35)), 100, 100);
        } catch (Exception error) { status.setText("Local frequency index could not start: " + error.getMessage()); }
        directoryController = new LocationDirectoryController(preferences, configuredSystems, configuredTalkgroups,
                p25SystemStore, p25TalkgroupStore, new RadioReferenceDirectoryClient(), audit, this::savedLocation,
                () -> shell.getScene().getWindow(), status::setText, () -> show("Trunking Workstation"));
        scanAutomation = new ScanAutomationController(preferences, () -> devices, this::savedLocation, configuredSystems,
                p25SystemStore, localSurvey, audit, this::closeReceiver, this::autoConfigureP25, status::setText,
                this::navigateTo, this::refreshLocalChannels);
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
        var requestedPage = System.getProperty("sdrpole.startPage", "Home");
        if (!"Home".equals(requestedPage) && navigation.getItems().contains(requestedPage)) navigateTo(requestedPage);
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
                "Home", "Trunking Workstation", "Scanner", "Frequency Library",
                "Calls & Recordings", "Setup & Diagnostics");
        navigation = new ListView<>(items);
        navigation.setAccessibleText("Main navigation");
        navigation.setAccessibleHelp("Use arrow keys to choose a workspace, then press Enter.");
        var nav = navigation;
        nav.setPrefWidth(180);
        nav.getSelectionModel().selectFirst();
        nav.setStyle("-fx-background-color: #0f1820; -fx-control-inner-background: #0f1820; " +
                "-fx-font-size: 14px; -fx-border-color: transparent #263540 transparent transparent;");
        nav.getSelectionModel().selectedItemProperty().addListener((o, old, selected) -> show(selected));
        return nav;
    }

    private Node statusBar() {
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var diagnose = new Button("Explain status");
        diagnose.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ACCENT + "; -fx-font-weight: bold;");
        diagnose.setOnAction(event -> navigateTo("Diagnostics"));
        var bar = new HBox(status, spacer, diagnose);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(9, 18, 9, 18));
        bar.setStyle("-fx-background-color: #0f1820; -fx-border-color: #263540 transparent transparent transparent;");
        return bar;
    }

    private void show(String page) {
        if (page == null) return;
        currentPage = page;
        shell.setCenter(switch (page) {
            case "Home" -> home();
            case "Trunking Workstation" -> trunkingWorkstationPage();
            case "Scanner" -> scannerPage();
            case "Frequency Library" -> frequencyLibraryPage();
            case "Calls & Recordings" -> listeningHealthPage();
            case "Setup & Diagnostics" -> setupPage();
            case "Devices" -> devicesPage();
            case "Nearby" -> nearbyPage();
            case "Decoders" -> decodersPage();
            case "Systems" -> systemsPage();
            case "Live Calls" -> listeningHealthPage();
            case "Spectrum" -> spectrumPage();
            case "Recordings" -> featurePage("Recordings", "Search calls by system, talkgroup, radio, or time",
                    "Automatic naming", "Retention rules", "Export audio", "Call metadata");
            case "Map" -> mapPage();
            case "Diagnostics" -> diagnosticsPage();
            default -> featurePage("Settings", "Sensible defaults first; advanced controls when needed",
                    "Audio output", "Storage", "Updates", "Privacy");
        });
    }

    private Node home() {
        var title = label("Choose one place to start", 30, true);
        var subtitle = muted("Connect a radio, pick your location, and let SDR-Pole build the listening setup—no blank frequency exam.");

        var quick = actionCard("Scan frequencies near me", "Choose named ranges such as airband, weather, marine, amateur, or public safety.",
                devices.isEmpty() ? "Radio setup is included" : "Ready with " + friendlyDeviceName(devices.getFirst()), "Open Scanner", () -> {
                    preferredDevice = devices.isEmpty() ? null : devices.getFirst();
                    navigateTo("Scanner");
                });
        var trunked = actionCard("Listen to local trunked radio", "Pick a location on the map, load directory records, choose a site, and listen.",
                configuredSystems.isEmpty() ? "Add your first site" : configuredSystems.size() + " site(s) saved",
                "Open Workstation", () -> navigateTo("Trunking Workstation"));
        HBox.setHgrow(quick, Priority.ALWAYS);
        HBox.setHgrow(trunked, Priority.ALWAYS);

        var readiness = new VBox(8,
                readinessRow("Radio connected", !devices.isEmpty(), devices.isEmpty() ? discovery.lastDiagnostic() : devices.size() + " detected"),
                readinessRow("Audio output", hasAudioOutput(), hasAudioOutput() ? "System output is available" : "No compatible output line found"),
                readinessRow("Analog listening", !devices.isEmpty(), devices.isEmpty() ? "Waiting for a radio" : "Ready now"),
                readinessRow("JMBE voice library", libraries.jmbePath().isPresent(), libraries.jmbePath().isPresent() ? "Installed" : "Optional—install from Decoders"),
                readinessRow("P25 trunking", false, p25Engine.enginePath().isPresent()
                        ? "Engine package installed; runtime bridge still pending" : "Protocol engine package is not installed"));
        var refresh = secondaryButton(devices.isEmpty() ? "Find my radio" : "Refresh radios");
        refresh.setOnAction(event -> refreshDevices(false));

        return page(new VBox(18, title, subtitle, new HBox(14, quick, trunked),
                new Separator(), label("What SDR-Pole handles", 19, true),
                muted("Radio detection • location • named frequency guides • decoder packages • safe tuner settings • audio checks"), readiness, refresh));
    }

    private Node trunkingWorkstationPage() {
        return page(new TrunkingWorkstationPane(devices, configuredSystems, savedLocation(), point -> {
            preferences.put("location.latitude", Double.toString(point.latitude()));
            preferences.put("location.longitude", Double.toString(point.longitude()));
            refreshLocalChannels(point);
            status.setText("Location saved—directory results will use this map point");
            audit.record("configuration", "listening area changed", "success", Map.of("location", "redacted"));
            directoryController.refreshIfNeeded(point);
        }, () -> refreshDevices(false), () -> navigateTo("Systems"), directoryController::connect,
                directoryController.summary(), scanAutomation::discoverP25, this::autoConfigureP25,
                () -> navigateTo("Live Calls")));
    }

    private void autoConfigureP25() {
        try {
            var plan = p25Configurator.create(devices, configuredSystems, savedLocation().orElse(null), configuredTalkgroups);
            var engine = p25Engine.enginePath().orElseThrow(() ->
                    new IllegalStateException("Install the P25 engine from Decoder packages first"));
            p25Runtime.start(engine, plan);
            preferences.put("p25.active.system", plan.system().systemName());
            preferences.put("p25.active.site", plan.system().siteName());
            preferences.putLong("p25.center.frequency", plan.centerFrequencyHz());
            preferences.putInt("p25.sample.rate", plan.sampleRate());
            audit.record("trunking", "automatic P25 configuration", "success", Map.of(
                    "deviceCount", devices.size(), "controlChannels", plan.system().controlFrequenciesHz().size(),
                    "simulcast", plan.system().modulation() == P25SystemConfig.Modulation.LSM_SIMULCAST));

            var detail = new StringBuilder()
                    .append("System: ").append(plan.system().systemName()).append(" / ").append(plan.system().siteName()).append('\n')
                    .append("Radio: ").append(friendlyDeviceName(plan.controlDevice())).append('\n')
                    .append("Control channels: ").append(plan.system().controlFrequenciesHz().size()).append('\n')
                    .append("Named talkgroups: ").append(plan.talkgroupCount()).append('\n')
                    .append("Automatic frequency correction: on\n")
                    .append("Unsafe RF power and bias tee: off\n\n");
            plan.notes().forEach(note -> detail.append("✓ ").append(note).append('\n'));
            var alert = new Alert(Alert.AlertType.INFORMATION, detail.toString(), ButtonType.OK);
            alert.setTitle("P25 is configured");
            alert.setHeaderText("SDR-Pole started local P25 monitoring");
            alert.initOwner(shell.getScene().getWindow());
            alert.showAndWait();
            status.setText("P25 monitoring: " + plan.system().systemName() + " / " + plan.system().siteName());
            show("Trunking Workstation");
        } catch (Exception problem) {
            audit.record("trunking", "automatic P25 configuration", "failure",
                    Map.of("errorType", problem.getClass().getSimpleName()));
            var alert = new Alert(Alert.AlertType.WARNING, problem.getMessage(), ButtonType.OK);
            alert.setTitle("P25 setup needs one thing");
            alert.setHeaderText("Automatic setup could not start yet");
            alert.initOwner(shell.getScene().getWindow());
            alert.showAndWait();
            status.setText(problem.getMessage());
        }
    }

    private Node scannerPage() {
        return page(new ScannerPane(devices, savedLocation(), localChannels, () -> navigateTo("Trunking Workstation"),
                scanAutomation::listen, bands -> {
                    scanAutomation.scan(bands);
                    status.setText("Scanner loaded " + bands.size() + " named range(s)");
                }, scanAutomation::hardwareSweep, status::setText));
    }

    private void refreshLocalChannels(GeoPoint point) {
        try { localChannels = frequencyDatabase.channelsNear(point, 100, 250); }
        catch (Exception error) { status.setText("Local frequency search failed: " + error.getMessage()); }
    }

    private Node frequencyLibraryPage() {
        try { return page(new FrequencyLibraryPane(frequencyDatabase.countsBySource())); }
        catch (Exception error) { return page(new FrequencyLibraryPane(Map.of())); }
    }

    private Node setupPage() {
        var radio = primaryButton("Radios"); radio.setOnAction(e -> navigateTo("Devices"));
        var decoders = primaryButton("Decoder packages"); decoders.setOnAction(e -> navigateTo("Decoders"));
        var diagnostics = primaryButton("Run diagnostics"); diagnostics.setOnAction(e -> navigateTo("Diagnostics"));
        return page(new VBox(16, label("Setup & Diagnostics", 30, true),
                muted("Setup is kept out of listening workflows unless something needs attention."),
                readinessRow("Radio", !devices.isEmpty(), devices.isEmpty() ? discovery.lastDiagnostic() : devices.size() + " detected"),
                readinessRow("Audio", hasAudioOutput(), hasAudioOutput() ? "Ready" : "Needs attention"),
                readinessRow("Voice library", libraries.jmbePath().isPresent(), libraries.jmbePath().isPresent() ? "JMBE installed" : "Install JMBE"),
                new HBox(10, radio, decoders, diagnostics)));
    }

    private java.util.Optional<GeoPoint> savedLocation() {
        try {
            var lat = preferences.get("location.latitude", "");
            var lon = preferences.get("location.longitude", "");
            return lat.isBlank() || lon.isBlank() ? java.util.Optional.empty()
                    : java.util.Optional.of(new GeoPoint(Double.parseDouble(lat), Double.parseDouble(lon)));
        } catch (RuntimeException ignored) { return java.util.Optional.empty(); }
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

    private Node systemsPage() {
        var title = label("Your radio systems", 28, true);
        var subtitle = muted("A system keeps its sites, control channels, map locations, and future talkgroups together—no playlist archaeology.");
        var add = primaryButton("Add P25 site");
        add.setOnAction(event -> showP25SiteWizard(null));
        var importButton = secondaryButton("Import P25 sites from CSV");
        importButton.setOnAction(event -> importP25Csv());
        var actions = new HBox(10, add, importButton);
        var systems = new VBox(10);
        if (configuredSystems.isEmpty()) {
            systems.getChildren().add(emptyState("No systems yet", "Use Add P25 site. SDR-Pole asks only for a name and control frequency; location is optional."));
        } else {
            configuredSystems.forEach(config -> systems.getChildren().add(systemCard(config)));
        }
        var health = secondaryButton("Why can't I hear calls?");
        health.setOnAction(event -> navigateTo("Live Calls"));
        return page(new VBox(16, title, subtitle, actions, systems, health));
    }

    private void importP25Csv() {
        var chooser = new FileChooser();
        chooser.setTitle("Import P25 sites");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV frequency lists", "*.csv"));
        var file = chooser.showOpenDialog(shell.getScene().getWindow());
        if (file == null) return;
        try {
            var imported = new P25CsvImporter().read(file.toPath());
            for (var site : imported) {
                configuredSystems.removeIf(existing -> existing.systemName().equalsIgnoreCase(site.systemName())
                        && existing.siteName().equalsIgnoreCase(site.siteName()));
                configuredSystems.add(site);
            }
            p25SystemStore.save(configuredSystems);
            audit.record("trunking", "P25 CSV import", "success", Map.of("siteCount", imported.size()));
            status.setText("Imported " + imported.size() + " P25 site(s)—automatic setup is ready");
            show("Systems");
        } catch (Exception problem) {
            audit.record("trunking", "P25 CSV import", "failure", Map.of("errorType", problem.getClass().getSimpleName()));
            var alert = new Alert(Alert.AlertType.WARNING, problem.getMessage(), ButtonType.OK);
            alert.setTitle("Could not import P25 sites");
            alert.setHeaderText("The frequency list needs attention");
            alert.initOwner(shell.getScene().getWindow());
            alert.showAndWait();
        }
    }

    private Node systemCard(P25SystemConfig config) {
        var frequency = config.controlFrequenciesHz().getFirst() / 1_000_000.0;
        var location = config.location() == null ? "Location not added" :
                String.format("%.4f, %.4f", config.location().latitude(), config.location().longitude());
        var summary = muted("Site: " + config.siteName() + "  •  Control: " + String.format("%.5f MHz", frequency)
                + "  •  " + (config.modulation() == P25SystemConfig.Modulation.LSM_SIMULCAST ? "Simulcast/LSM" : "C4FM")
                + "  •  " + location);
        summary.setWrapText(true);
        var copy = new VBox(5, label(config.systemName(), 18, true), summary);
        HBox.setHgrow(copy, Priority.ALWAYS);
        var map = secondaryButton(config.location() == null ? "Add location" : "Map");
        map.setOnAction(event -> {
            if (config.location() == null) showP25SiteWizard(config);
            else {
                if (siteMap == null) siteMap = new SiteMapView();
                siteMap.centerOn(config.location(), 11);
                navigateTo("Map");
            }
        });
        var edit = secondaryButton("Edit");
        edit.setOnAction(event -> showP25SiteWizard(config));
        var remove = secondaryButton("Remove");
        remove.setOnAction(event -> removeSystem(config));
        var row = new HBox(12, copy, map, edit, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(16));
        row.setStyle(panelStyle());
        return row;
    }

    private void showP25SiteWizard(P25SystemConfig existing) {
        var dialog = new Dialog<P25SystemConfig>();
        dialog.setTitle(existing == null ? "Add a P25 site" : "Edit P25 site");
        dialog.setHeaderText("Three plain-language decisions—SDR-Pole handles the decoder details");
        dialog.initOwner(shell.getScene().getWindow());
        var saveType = new ButtonType("Save site", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, saveType);

        var systemName = new TextField(existing == null ? "" : existing.systemName());
        systemName.setPromptText("County Public Safety");
        var siteName = new TextField(existing == null ? "" : existing.siteName());
        siteName.setPromptText("North Site");
        var control = new TextField(existing == null ? "" : String.format("%.5f", existing.controlFrequenciesHz().getFirst() / 1_000_000.0));
        control.setPromptText("851.11250");
        var simulcast = new CheckBox("This is a simulcast site");
        simulcast.setStyle("-fx-text-fill: #17242d;");
        simulcast.setSelected(existing != null && existing.modulation() == P25SystemConfig.Modulation.LSM_SIMULCAST);
        simulcast.setTooltip(new Tooltip("Choose this only when the system directory calls the site simulcast; otherwise leave it off."));
        var latitude = new TextField(existing != null && existing.location() != null ? Double.toString(existing.location().latitude()) : preferences.get("location.latitude", ""));
        var longitude = new TextField(existing != null && existing.location() != null ? Double.toString(existing.location().longitude()) : preferences.get("location.longitude", ""));
        latitude.setPromptText("Optional"); longitude.setPromptText("Optional");
        var error = new Label("");
        error.setStyle("-fx-text-fill: #b42318; -fx-font-weight: bold;");
        error.setWrapText(true);

        var form = new VBox(10,
                dialogSection("1  Name it", "Use names you recognize; protocol identifiers will be learned from the control channel.",
                        new HBox(10, dialogField("System", systemName), dialogField("Site", siteName))),
                dialogSection("2  Add the control signal", "Phase 1 and Phase 2 systems use a Phase 1 control channel. Start with its primary frequency.",
                        new HBox(10, dialogField("Control frequency (MHz)", control), simulcast)),
                dialogSection("3  Put it on the map (optional)", "Coordinates help SDR-Pole choose nearby sites; they are stored only on this computer.",
                        new HBox(10, dialogField("Latitude", latitude), dialogField("Longitude", longitude))),
                error);
        form.setPrefWidth(680);
        dialog.getDialogPane().setContent(form);

        var saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try { createP25Config(systemName, siteName, control, simulcast, latitude, longitude); }
            catch (RuntimeException problem) { error.setText(problem.getMessage()); event.consume(); }
        });
        dialog.setResultConverter(button -> button == saveType
                ? createP25Config(systemName, siteName, control, simulcast, latitude, longitude) : null);
        dialog.showAndWait().ifPresent(config -> {
            try {
                if (existing != null) configuredSystems.remove(existing);
                configuredSystems.removeIf(item -> item.systemName().equalsIgnoreCase(config.systemName())
                        && item.siteName().equalsIgnoreCase(config.siteName()));
                configuredSystems.add(config);
                p25SystemStore.save(configuredSystems);
                status.setText("Saved " + config.systemName() + " / " + config.siteName());
                show("Systems");
            } catch (Exception problem) {
                status.setText("Could not save the site: " + problem.getMessage());
            }
        });
    }

    private P25SystemConfig createP25Config(TextField system, TextField site, TextField control,
                                             CheckBox simulcast, TextField latitude, TextField longitude) {
        long frequency;
        try { frequency = Math.round(Double.parseDouble(control.getText().trim()) * 1_000_000); }
        catch (NumberFormatException error) { throw new IllegalArgumentException("Enter a control frequency such as 851.11250 MHz"); }
        GeoPoint point = null;
        if (!latitude.getText().isBlank() || !longitude.getText().isBlank()) {
            if (latitude.getText().isBlank() || longitude.getText().isBlank())
                throw new IllegalArgumentException("Enter both latitude and longitude, or leave both blank");
            try { point = new GeoPoint(Double.parseDouble(latitude.getText().trim()), Double.parseDouble(longitude.getText().trim())); }
            catch (NumberFormatException error) { throw new IllegalArgumentException("Latitude and longitude must be numbers"); }
        }
        return new P25SystemConfig(system.getText().trim(), site.getText().trim(), List.of(frequency),
                simulcast.isSelected() ? P25SystemConfig.Modulation.LSM_SIMULCAST : P25SystemConfig.Modulation.C4FM,
                3, point);
    }

    private VBox dialogSection(String title, String detail, Node content) {
        var description = new Label(detail);
        description.setWrapText(true);
        description.setStyle("-fx-text-fill: #4f6470;");
        var box = new VBox(5, new Label(title), description, content);
        ((Label) box.getChildren().getFirst()).setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #f3f7f9; -fx-background-radius: 8;");
        return box;
    }

    private VBox dialogField(String title, Node control) { return new VBox(3, new Label(title), control); }

    private void removeSystem(P25SystemConfig config) {
        var alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + config.systemName() + " / " + config.siteName() + "? This does not affect the radio or decoder libraries.",
                ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle("Remove P25 site");
        alert.setHeaderText("Remove this saved site?");
        alert.showAndWait().filter(ButtonType.OK::equals).ifPresent(button -> {
            try {
                configuredSystems.remove(config);
                p25SystemStore.save(configuredSystems);
                status.setText("Removed " + config.systemName() + " / " + config.siteName());
                show("Systems");
            } catch (Exception problem) { status.setText("Could not remove site: " + problem.getMessage()); }
        });
    }

    private Node listeningHealthPage() {
        boolean radio = !devices.isEmpty();
        boolean site = !configuredSystems.isEmpty();
        boolean voice = libraries.jmbePath().isPresent();
        var title = label("Listening health", 28, true);
        var subtitle = muted("Instead of silent failure, follow the signal from antenna to speaker and fix the first incomplete step.");
        var path = new VBox(8,
                readinessRow("1  Radio", radio, radio ? friendlyDeviceName(devices.getFirst()) + " detected" : discovery.lastDiagnostic()),
                readinessRow("2  Protected RF settings", radio, radio ? "Safe device profile will be applied" : "Waiting for a radio"),
                readinessRow("3  P25 site", site, site ? configuredSystems.size() + " saved site(s)" : "Add a system and control channel"),
                readinessRow("4  Control-channel signal", false, site ? "Symbol/frame decoder is not implemented yet" : "Needs a configured site first"),
                readinessRow("5  Talkgroup grant", false, "Waiting for the control-channel decoder"),
                readinessRow("6  Voice library", voice, voice ? "JMBE installed" : "Install JMBE from Decoders"),
                readinessRow("7  Speaker", hasAudioOutput(), hasAudioOutput() ? "Audio output available" : "No compatible output"));
        var fix = primaryButton(!radio ? "Connect a radio" : !site ? "Add a P25 site" : !voice ? "Install JMBE" : "View decoder status");
        fix.setOnAction(event -> navigateTo(!radio ? "Devices" : !site ? "Systems" : !voice ? "Decoders" : "Diagnostics"));
        var analog = secondaryButton("Analog listening works now");
        analog.setOnAction(event -> navigateTo("Spectrum"));
        var note = muted("Seeing call metadata without audio is not treated as success. Future decoder stages will report sync, CRC, grant, encryption, vocoder, and audio state independently.");
        note.setWrapText(true);
        return page(new VBox(15, title, subtitle, path, new HBox(10, fix, analog), note));
    }

    private Node mapPage() {
        if (siteMap == null) siteMap = new SiteMapView();
        for (var config : configuredSystems) {
            if (config.location() != null) siteMap.addMarker(new SiteMapView.SiteMarker(
                    config.systemName() + " / " + config.siteName(), config.location(), config.controlFrequenciesHz().getFirst()));
        }
        var title = label("System site map", 28, true);
        var subtitle = muted("Drag to pan, scroll to zoom, and center the map on coordinates. Site markers can carry control-channel configuration.");
        var latitude = new TextField(preferences.get("location.latitude", "39.5"));
        var longitude = new TextField(preferences.get("location.longitude", "-98.35"));
        latitude.setPrefWidth(120); longitude.setPrefWidth(120);
        var center = primaryButton("Center map");
        var mapState = muted("Map data © OpenStreetMap contributors • visible tiles are cached locally for at least seven days.");
        center.setOnAction(event -> {
            try {
                var point = new GeoPoint(Double.parseDouble(latitude.getText().trim()), Double.parseDouble(longitude.getText().trim()));
                siteMap.centerOn(point, 10);
                preferences.put("location.latitude", latitude.getText().trim());
                preferences.put("location.longitude", longitude.getText().trim());
                mapState.setText(String.format("Centered at %.5f, %.5f", point.latitude(), point.longitude()));
            } catch (RuntimeException error) {
                mapState.setText("Enter valid latitude and longitude");
            }
        });
        var controls = new HBox(10, field("Latitude", latitude), field("Longitude", longitude), center);
        controls.setAlignment(Pos.BOTTOM_LEFT);
        var truth = muted("Markers show documented or user-entered site locations—not positions inferred from one HackRF. Direction finding needs bearings from multiple observations or coherent receivers.");
        truth.setWrapText(true);
        return page(new VBox(12, title, subtitle, controls, mapState, siteMap, truth));
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
        var genericGain = field("Generic tuner gain", new VBox(2, gain, gainLabel));

        var lna = steppedSlider(0, 40, 16, 8);
        var lnaLabel = muted("LNA 16 dB");
        lna.valueProperty().addListener((o, old, value) -> lnaLabel.setText("LNA " + Math.round(value.doubleValue() / 8) * 8 + " dB"));
        var vga = steppedSlider(0, 62, 16, 2);
        var vgaLabel = muted("VGA 16 dB");
        vga.valueProperty().addListener((o, old, value) -> vgaLabel.setText("VGA " + Math.round(value.doubleValue() / 2) * 2 + " dB"));
        var rfAmp = new CheckBox("RF amp (+~11 dB)");
        var antennaPower = new CheckBox("Antenna power (3.3 V)");
        for (var box : List.of(rfAmp, antennaPower)) box.setStyle("-fx-text-fill: " + TEXT + ";");
        var riskConfirmed = new SimpleBooleanProperty(false);
        rfAmp.setOnAction(event -> confirmHighRisk(rfAmp, riskConfirmed, "Enable HackRF RF amplifier?",
                "The amplifier is only for weak signals. HackRF's maximum safe input is -5 dBm and software cannot detect excessive external RF. Use attenuation near transmitters."));
        antennaPower.setOnAction(event -> confirmHighRisk(antennaPower, riskConfirmed, "Enable antenna-port power?",
                "This supplies approximately 3.0–3.3 V with a 50 mA maximum. Enable it only for compatible active antennas or external LNAs. It can damage incompatible or shorted accessories."));
        var protectedControls = new HBox(10, field("HackRF IF gain", new VBox(2, lna, lnaLabel)),
                field("HackRF baseband gain", new VBox(2, vga, vgaLabel)), rfAmp, antennaPower);
        protectedControls.setAlignment(Pos.BOTTOM_LEFT);
        var safety = muted("Protection: RF amp OFF • antenna power OFF • LNA/VGA vendor-clamped • maximum input -5 dBm");
        safety.setWrapText(true);

        Runnable updateHardwareControls = () -> {
            boolean hackrf = deviceChoice.getValue() != null && "hackrf".equalsIgnoreCase(deviceChoice.getValue().driver());
            protectedControls.setManaged(hackrf);
            protectedControls.setVisible(hackrf);
            safety.setManaged(hackrf);
            safety.setVisible(hackrf);
            genericGain.setManaged(!hackrf);
            genericGain.setVisible(!hackrf);
            automaticGain.setDisable(hackrf);
            if (hackrf) automaticGain.setSelected(false);
        };
        deviceChoice.valueProperty().addListener((o, old, selected) -> updateHardwareControls.run());
        updateHardwareControls.run();

        var essentials = new HBox(10, field("Radio", deviceChoice), field("Frequency (MHz)", frequency), field("Mode", mode));
        essentials.setAlignment(Pos.BOTTOM_LEFT);
        var advancedControls = new VBox(8, new HBox(10, field("Sample rate", sampleRate), genericGain, automaticGain),
                protectedControls, safety);
        advancedControls.setAlignment(Pos.BOTTOM_LEFT);
        var controls = new VBox(10, field("Quick examples", preset), essentials);
        if (advancedMode) controls.getChildren().add(advancedControls);
        else controls.getChildren().add(muted("Recommended settings: 2 MS/s • 24 dB gain • mono system audio"));

        var waterfall = new WaterfallView(900, 300);
        var initialHz = new AtomicLong(Math.round(Double.parseDouble(frequency.getText().trim()) * 1_000_000));
        waterfall.setTuning(initialHz.get(), sampleRate.getValue());
        var signalAssist = new SignalAssistPane();
        boolean requestedRangeScan = preferences.getBoolean("scanner.auto", false);
        var scanRanges = ScanPlan.decode(preferences.get("scanner.ranges", ""));
        var scanSpeed = new ComboBox<ScanSpeed>();
        scanSpeed.getItems().addAll(ScanSpeed.values());
        try { scanSpeed.getSelectionModel().select(ScanSpeed.valueOf(preferences.get("scanner.speed", ScanSpeed.FAST.name()))); }
        catch (IllegalArgumentException ignored) { scanSpeed.getSelectionModel().select(ScanSpeed.FAST); }
        scanSpeed.valueProperty().addListener((o, old, value) -> preferences.put("scanner.speed", value.name()));
        var rangeScanning = new AtomicBoolean(false);
        signalAssist.setAutoTuneEnabled(requestedRangeScan);
        var receiverStatus = label("Stopped", 15, true);
        var level = new ProgressBar(0);
        level.setPrefWidth(180);
        var listen = primaryButton("Listen");
        var stop = secondaryButton("Stop");
        stop.setDisable(true);

        java.util.function.LongConsumer tuneTo = hz -> {
            if (hz <= 0) return;
            initialHz.set(hz);
            frequency.setText(String.format("%.5f", hz / 1_000_000.0));
            waterfall.setTuning(hz, sampleRate.getValue());
            var receiver = activeReceiver;
            if (receiver != null) receiver.tune(hz, mode.getValue());
        };
        signalAssist.setOnTuneDelta(delta -> tuneTo.accept(initialHz.get() + delta));
        waterfall.setOnTune(tuneTo);
        signalAssist.setOnAutoTune(signal -> {
            rangeScanning.set(false);
            mode.getSelectionModel().select(signal.hint().mode());
            tuneTo.accept(signal.frequencyHz());
            receiverStatus.setText("Auto-tuned " + String.format("%.5f MHz", signal.frequencyHz() / 1e6));
            Thread.startVirtualThread(() -> {
                try {
                    localSurvey.record(signal, savedLocation().orElse(null));
                    savedLocation().ifPresent(point -> Platform.runLater(() -> refreshLocalChannels(point)));
                } catch (Exception problem) {
                    Platform.runLater(() -> status.setText("Signal heard, but survey history could not save: " + problem.getMessage()));
                }
            });
            scanAutomation.validateP25Candidate(signal);
        });
        sampleRate.valueProperty().addListener((o, old, value) -> waterfall.setTuning(initialHz.get(), value));

        listen.setOnAction(event -> {
            var selected = deviceChoice.getValue();
            if (selected == null) {
                receiverStatus.setText("Connect and select an SDR first");
                return;
            }
            try {
                long hz = Math.round(Double.parseDouble(frequency.getText().trim()) * 1_000_000);
                initialHz.set(hz);
                waterfall.setTuning(hz, sampleRate.getValue());
                preferences.put("lastFrequencyMhz", frequency.getText().trim());
                preferences.put("lastMode", mode.getValue().name());
                closeReceiver();
                var frontend = "hackrf".equalsIgnoreCase(selected.driver())
                        ? new RfFrontendSettings(Math.round(lna.getValue() / 8) * 8,
                        Math.round(vga.getValue() / 2) * 2, rfAmp.isSelected(), antennaPower.isSelected(), riskConfirmed.get())
                        : RfFrontendSettings.safeDefaults(selected);
                activeReceiver = new LiveNfmReceiver(
                        new ReceiverConfig(selected, hz, sampleRate.getValue(), gain.getValue(), automaticGain.isSelected(),
                                48_000, mode.getValue(), frontend),
                        new ReceiverListener() {
                            @Override public void onStatus(String message) { Platform.runLater(() -> receiverStatus.setText(message)); }
                            @Override public void onSpectrum(float[] powerDb) {
                                waterfall.accept(powerDb);
                                signalAssist.analyze(powerDb, initialHz.get(), sampleRate.getValue());
                            }
                            @Override public void onLevel(double rms) { Platform.runLater(() -> level.setProgress(Math.min(1, rms * 5))); }
                            @Override public void onError(String friendlyMessage, Throwable cause) {
                                Platform.runLater(() -> receiverStatus.setText(friendlyMessage));
                            }
                        });
                activeReceiver.start();
                if (requestedRangeScan && !scanRanges.isEmpty()) {
                    var fastPlan = new FastScanPlan(scanRanges, sampleRate.getValue());
                    int dwellMilliseconds = scanSpeed.getValue().dwellMilliseconds();
                    rangeScanning.set(true);
                    receiverStatus.setText("Fast scanning " + fastPlan.windowCount() + " FFT windows");
                    Thread.ofVirtual().start(() -> {
                        while (rangeScanning.get() && activeReceiver != null) {
                            var step = fastPlan.next();
                            Platform.runLater(() -> {
                                mode.getSelectionModel().select(step.mode());
                                tuneTo.accept(step.centerFrequencyHz());
                                receiverStatus.setText("Fast sweep • range " + step.rangeNumber() + "/" + step.rangeCount()
                                        + " • " + fastPlan.windowCount() + " windows");
                            });
                            try { Thread.sleep(dwellMilliseconds); }
                            catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); break; }
                        }
                    });
                    preferences.putBoolean("scanner.auto", false);
                }
                listen.setDisable(true);
                stop.setDisable(false);
            } catch (NumberFormatException e) {
                receiverStatus.setText("Enter a frequency such as 155.25000");
            } catch (RuntimeException e) {
                receiverStatus.setText(e.getMessage());
            }
        });
        stop.setOnAction(event -> {
            rangeScanning.set(false);
            closeReceiver();
            listen.setDisable(false);
            stop.setDisable(true);
            receiverStatus.setText("Stopped");
        });

        var transport = new HBox(10, listen, stop, field("Scan speed", scanSpeed), receiverStatus, muted("Audio level"), level);
        transport.setAlignment(Pos.CENTER_LEFT);
        var help = muted(advancedMode
                ? "P25 requires a frame decoder plus a compatible voice package; JMBE alone is not a signal decoder."
                : "Not sure where to begin? Try a NOAA Weather example. Reception depends on your location, antenna, and local signals.");
        help.setWrapText(true);
        return page(new VBox(16, title, subtitle, controls, signalAssist, waterfall, transport, help));
    }

    private Node decoderRow(DecoderCatalogEntry decoder) {
        var name = label(decoder.name(), 17, true);
        var description = muted(decoder.summary());
        description.setWrapText(true);
        var text = new VBox(4, name, description, muted("License: " + decoder.license()));
        HBox.setHgrow(text, Priority.ALWAYS);
        if (decoder.id().equals("jmbe")) return jmbeRow(decoder, text);
        if (decoder.id().startsWith("p25-phase")) return p25EngineRow(decoder, text);
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

    private Node p25EngineRow(DecoderCatalogEntry decoder, VBox text) {
        var installed = p25Engine.enginePath();
        if (installed.isPresent()) {
            text.getChildren().add(accent("Package installed: GopherTrunk engine " + p25Engine.version()));
            text.getChildren().add(muted("Runtime bridge pending—this is not yet an operational decoder status."));
        }
        else text.getChildren().add(muted("Requires a compatible external protocol engine; JMBE alone is voice conversion."));
        var choose = installed.isPresent() ? secondaryButton("Package installed") : primaryButton("Select engine");
        choose.setDisable(installed.isPresent());
        choose.setOnAction(event -> {
            var picker = new FileChooser();
            picker.setTitle("Select GopherTrunk executable");
            var selected = picker.showOpenDialog(shell.getScene().getWindow());
            if (selected == null) return;
            try {
                p25Engine.installGopherTrunk(selected.toPath(), "0.4.8");
                status.setText("P25 Phase 1 and Phase 2 engine installed and registered");
                show("Decoders");
            } catch (Exception error) { status.setText("P25 engine installation failed: " + error.getMessage()); }
        });
        var row = new HBox(14, text, choose);
        row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(16)); row.setStyle(panelStyle());
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
                    audit.record("package", "JMBE install", "success", Map.of("version", JmbeInstaller.VERSION));
                });
            } catch (Exception error) {
                Platform.runLater(() -> {
                    progress.setVisible(false);
                    progress.setManaged(false);
                    download.setDisable(false);
                    status.setText("JMBE installation failed: " + error.getMessage());
                    audit.record("package", "JMBE install", "failure", Map.of("errorType", error.getClass().getSimpleName()));
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
                check("P25 runtime bridge", false, p25Engine.enginePath().isPresent()
                        ? "GopherTrunk package registered; SDR-Pole process integration remains unfinished"
                        : "No P25 protocol engine package is registered"),
                check("Local audit log", true, "Sensitive fields are redacted • " + audit.path().getFileName()));
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
                P25 engine package: %s
                P25 runtime bridge: not operational
                """.formatted(
                System.getProperty("os.name"), System.getProperty("os.arch"), System.getProperty("java.version"),
                devices.size(), discovery.lastDiagnostic(), hasAudioOutput() ? "available" : "unavailable",
                libraries.jmbePath().isPresent() ? "installed" : "not installed",
                p25Engine.enginePath().isPresent() ? "registered" : "not installed");
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
            audit.record("hardware", "device discovery", found.isEmpty() ? "no-device" : "success", Map.of("deviceCount", found.size()));
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
        if (!navigation.getItems().contains(page)) {
            navigation.getSelectionModel().clearSelection();
            show(page);
            return;
        }
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

    private Slider steppedSlider(double minimum, double maximum, double value, double step) {
        var slider = new Slider(minimum, maximum, value);
        slider.setBlockIncrement(step);
        slider.setMajorTickUnit(step);
        slider.setSnapToTicks(true);
        slider.setPrefWidth(145);
        return slider;
    }

    private void confirmHighRisk(CheckBox control, SimpleBooleanProperty confirmed, String title, String detail) {
        if (!control.isSelected()) return;
        var alert = new Alert(Alert.AlertType.WARNING, detail, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle("RF hardware protection");
        alert.setHeaderText(title);
        boolean accepted = alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
        control.setSelected(accepted);
        if (accepted) confirmed.set(true);
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

    @Override public void stop() { closeReceiver(); p25Runtime.close(); }

    public static void main(String[] args) { launch(args); }
}
