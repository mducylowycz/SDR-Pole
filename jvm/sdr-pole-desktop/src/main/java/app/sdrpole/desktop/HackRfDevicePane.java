package app.sdrpole.desktop;

import app.sdrpole.core.SdrDevice;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** HackRF-specific inventory and safe receive capabilities; no mutating hardware operations live here. */
final class HackRfDevicePane extends VBox {
    HackRfDevicePane(SdrDevice device, Runnable configure) {
        setSpacing(10); setPadding(new Insets(16));
        setStyle("-fx-background-color:#14202a;-fx-background-radius:12;-fx-border-color:#263b48;-fx-border-radius:12;");
        var state = accent(device.available() ? "● READY" : "○ BUSY");
        var spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        var button = new Button("Open tuner"); button.setOnAction(event -> configure.run());
        button.setStyle("-fx-background-color:#20313d;-fx-text-fill:#edf7fa;-fx-padding:9 14;-fx-background-radius:8;");
        var header = new HBox(10, title(device.label(), 18), muted(shortSerial(device.serial())), spacer, state, button);
        header.setAlignment(Pos.CENTER_LEFT);

        var properties = device.properties();
        boolean pro = properties.getOrDefault("hackrf.board", device.label()).toLowerCase().contains("pro");
        var facts = new GridPane(); facts.setHgap(18); facts.setVgap(6);
        addFact(facts, 0, "Receive range", pro ? "100 kHz–6 GHz" : "1 MHz–6 GHz");
        addFact(facts, 1, "Samples", pro ? "8-bit IQ • optional 16/4-bit modes" : "8-bit signed IQ");
        addFact(facts, 2, "Native rate", pro ? "8–20 MS/s recommended • up to 40 MS/s half precision" : "8–20 MS/s recommended");
        addFact(facts, 3, "Architecture", "Half duplex • receive-only in SDR-Pole");

        var firmware = properties.getOrDefault("hackrf.firmware", "Connect and refresh to read");
        var host = properties.getOrDefault("hackrf.hostTools", "unknown");
        var api = properties.getOrDefault("hackrf.api", "unknown");
        var software = muted("Firmware " + firmware + " • API " + api + " • host tools " + host);
        var tools = new HBox(8, chip("Hardware sweep", enabled(properties, "hackrf.sweep")),
                chip("External clock", enabled(properties, "hackrf.clock")),
                chip("Opera Cake", enabled(properties, "hackrf.operacake")));
        var safety = muted(pro
                ? "Protected receive profile: enhanced RF protection • RF amp off • LNA 16 dB • VGA 16 dB • antenna power off. Use an external attenuator/filter near transmitters."
                : "Protected receive profile: maximum input −5 dBm • RF amp off • LNA 16 dB • VGA 16 dB • antenna power off. Use an external attenuator/filter near transmitters.");
        var findings = properties.getOrDefault("hackrf.findings", "");
        getChildren().addAll(header, facts, software, tools, safety);
        if (!findings.isBlank()) getChildren().add(muted(findings.replace('\n', ' ')));
    }

    private static void addFact(GridPane grid, int column, String name, String value) {
        grid.add(new VBox(2, muted(name), title(value, 13)), column % 2, column / 2);
    }
    private static Label chip(String name, boolean available) {
        var label = title((available ? "✓ " : "○ ") + name, 12);
        label.setStyle(label.getStyle() + "-fx-text-fill:" + (available ? "#63e6a5" : "#92a8b5") + ";-fx-background-color:#20313d;-fx-padding:5 8;-fx-background-radius:7;");
        return label;
    }
    private static boolean enabled(java.util.Map<String, String> properties, String key) { return Boolean.parseBoolean(properties.getOrDefault(key, "false")); }
    private static String shortSerial(String serial) { return serial == null || serial.isBlank() ? "serial unavailable" : "serial …" + serial.substring(Math.max(0, serial.length() - 8)); }
    private static Label title(String text, int size) { var label = new Label(text); label.setWrapText(true); label.setStyle("-fx-text-fill:#edf7fa;-fx-font-size:" + size + "px;-fx-font-weight:bold;"); return label; }
    private static Label muted(String text) { var label = new Label(text); label.setWrapText(true); label.setStyle("-fx-text-fill:#92a8b5;-fx-font-size:13px;"); return label; }
    private static Label accent(String text) { var label = title(text, 13); label.setStyle(label.getStyle() + "-fx-text-fill:#22c6da;"); return label; }
}
