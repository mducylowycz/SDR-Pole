package app.sdrpole.desktop;

import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.SdrDevice;
import app.sdrpole.core.p25.P25SystemConfig;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Map-first trunking workflow. Directory adapters remain separate data services. */
final class TrunkingWorkstationPane extends VBox {
    TrunkingWorkstationPane(List<SdrDevice> devices, List<P25SystemConfig> systems, Optional<GeoPoint> location,
                            Consumer<GeoPoint> locationSelected, Runnable refresh, Runnable manage,
                            Runnable updateDirectory, String directoryStatus,
                            Runnable autoConfigure, Runnable readiness) {
        setSpacing(14);
        var map = new SiteMapView(); location.ifPresent(point -> map.centerOn(point, 9));
        systems.stream().filter(config -> config.location() != null).forEach(config -> map.addMarker(
                new SiteMapView.SiteMarker(config.systemName() + " / " + config.siteName(), config.location(), config.controlFrequenciesHz().getFirst())));
        var locationState = accent(location.map(point -> String.format("Selected area: %.4f, %.4f", point.latitude(), point.longitude()))
                .orElse("Click the map to select your listening area"));
        map.setOnLocationSelected(point -> { locationSelected.accept(point); locationState.setText(String.format("Selected area: %.4f, %.4f", point.latitude(), point.longitude())); });
        var find = secondary(devices.isEmpty() ? "Find radio" : "Refresh"); find.setOnAction(e -> refresh.run());
        var radio = new HBox(10, accent(devices.isEmpty() ? "○  Radio" : "●  Radio"),
                muted(devices.isEmpty() ? "Connect a dongle, then refresh" : devices.getFirst().label()), find); radio.setAlignment(Pos.CENTER_LEFT);
        var sources = new VBox(8,
                source("Bundled frequency guide", "Installed offline; nationwide ranges and correct modes.", "READY"),
                source("Saved/imported systems", systems.size() + " site(s); located sites are pinned above.", "READY"),
                source("FCC ULS public records", "Free license records; a license is not proof a channel is active.", "Importer next"),
                source("RadioReference directory", "Official local trunked systems, sites, control channels, and modulation. " + directoryStatus,
                        directoryStatus.startsWith("Updated") ? "CURRENT" : "CONNECT"));
        var update = primary(directoryStatus.startsWith("Updated") ? "Update local radio data" : "Connect location data");
        update.setOnAction(event -> updateDirectory.run());
        var manageButton = primary(systems.isEmpty() ? "Add a system without a directory" : "Manage loaded systems"); manageButton.setOnAction(e -> manage.run());
        var automatic = primary("Auto-configure P25 & listen");
        automatic.setAccessibleHelp("Select the nearest saved P25 site, configure connected radios, and start the local decoder engine.");
        automatic.setDisable(devices.isEmpty() || systems.isEmpty());
        automatic.setOnAction(e -> autoConfigure.run());
        var readyButton = primary("Check listening readiness"); readyButton.setOnAction(e -> readiness.run());
        getChildren().addAll(title("Trunking Workstation", 30), muted("Radio, map, directories, sites, decoders, and listening status in one guided workspace."),
                title("1  Connect", 18), radio, title("2  Choose your area", 18), muted("Click once to pin your location. Drag to pan; scroll to zoom."), locationState, map,
                title("3  Load frequency libraries", 18), sources, update,
                title("4  Let SDR-Pole configure it", 18),
                muted("SDR-Pole chooses the nearest saved site, assigns every connected radio, enables safe gain and frequency correction, and starts P25 locally."),
                new HBox(10, automatic, manageButton, readyButton));
    }

    private static HBox source(String name, String detail, String state) {
        var copy = new VBox(2, title(name, 15), muted(detail)); javafx.scene.layout.HBox.setHgrow(copy, javafx.scene.layout.Priority.ALWAYS);
        var row = new HBox(10, copy, accent(state)); row.setAlignment(Pos.CENTER_LEFT); row.setStyle("-fx-background-color:#14202a;-fx-padding:12;-fx-background-radius:10;"); return row;
    }
    private static Button primary(String text) { var b = new Button(text); b.setStyle("-fx-background-color:#22c6da;-fx-text-fill:#061418;-fx-font-weight:bold;-fx-padding:10 16;-fx-background-radius:8;"); return b; }
    private static Button secondary(String text) { var b = new Button(text); b.setStyle("-fx-background-color:#20313d;-fx-text-fill:#edf7fa;-fx-padding:9 14;-fx-background-radius:8;"); return b; }
    private static Label title(String text, int size) { var l = new Label(text); l.setStyle("-fx-text-fill:#edf7fa;-fx-font-size:" + size + "px;-fx-font-weight:bold;"); return l; }
    private static Label muted(String text) { var l = new Label(text); l.setStyle("-fx-text-fill:#92a8b5;-fx-font-size:13px;"); l.setWrapText(true); return l; }
    private static Label accent(String text) { var l = title(text, 14); l.setStyle(l.getStyle() + "-fx-text-fill:#22c6da;"); return l; }
}
