package app.sdrpole.desktop;

import app.sdrpole.core.FrequencyBand;
import app.sdrpole.core.FrequencyBandCatalog;
import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.SdrDevice;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/** Goal-focused conventional scanner. It owns presentation, not radio or persistence. */
final class ScannerPane extends VBox {
    ScannerPane(List<SdrDevice> devices, Optional<GeoPoint> location, Runnable chooseLocation,
                Consumer<FrequencyBand> listen, Consumer<List<FrequencyBand>> scan, Consumer<String> notice) {
        setSpacing(14);
        var locationText = location.map(point -> String.format("Location: %.4f, %.4f", point.latitude(), point.longitude()))
                .orElse("No location selected yet");
        var map = secondary("Choose on map"); map.setOnAction(e -> chooseLocation.run());
        var locationRow = new HBox(10, accent(locationText), map); locationRow.setAlignment(Pos.CENTER_LEFT);
        var selected = new ArrayList<FrequencyBand>();
        var rows = new VBox(7);
        for (var band : FrequencyBandCatalog.northAmerica()) {
            var include = new CheckBox();
            include.selectedProperty().addListener((o, old, value) -> { if (value) selected.add(band); else selected.remove(band); });
            var copy = new VBox(2, title(band.name(), 15), muted(band.rangeLabel() + "  •  " + band.mode() + "  •  " + band.commonUse()));
            HBox.setHgrow(copy, Priority.ALWAYS);
            var now = secondary("Listen here"); now.setOnAction(e -> listen.accept(band));
            var row = new HBox(10, include, copy, now); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(10));
            row.setStyle("-fx-background-color:#14202a;-fx-background-radius:12;-fx-border-color:#263b48;-fx-border-radius:12;");
            rows.getChildren().add(row);
        }
        var auto = primary("Auto-scan selected ranges");
        auto.setOnAction(e -> { if (selected.isEmpty()) notice.accept("Select at least one named range first"); else scan.accept(List.copyOf(selected)); });
        var radio = status("Radio", !devices.isEmpty(), devices.isEmpty() ? "Connect one in Setup" : devices.getFirst().label());
        getChildren().addAll(title("Scanner", 30), muted("Select by purpose, not mystery numbers. Auto-scan cycles every selected range and stops on a stable strong signal."),
                locationRow, new HBox(10, radio, auto), title("Common frequency ranges", 19), rows);
    }

    private static HBox status(String name, boolean okay, String detail) {
        var dot = title(okay ? "●" : "○", 18); dot.setStyle("-fx-text-fill:" + (okay ? "#63e6a5" : "#ffb454") + ";");
        var row = new HBox(10, dot, title(name, 15), muted(detail)); row.setAlignment(Pos.CENTER_LEFT); row.setPadding(new Insets(12));
        row.setStyle("-fx-background-color:#14202a;-fx-background-radius:12;-fx-border-color:#263b48;-fx-border-radius:12;"); return row;
    }
    private static Button primary(String text) { var b = new Button(text); b.setStyle("-fx-background-color:#22c6da;-fx-text-fill:#061418;-fx-font-weight:bold;-fx-padding:10 16;-fx-background-radius:8;"); return b; }
    private static Button secondary(String text) { var b = new Button(text); b.setStyle("-fx-background-color:#20313d;-fx-text-fill:#edf7fa;-fx-padding:9 14;-fx-background-radius:8;"); return b; }
    private static Label title(String text, int size) { var l = new Label(text); l.setStyle("-fx-text-fill:#edf7fa;-fx-font-size:" + size + "px;-fx-font-weight:bold;"); return l; }
    private static Label muted(String text) { var l = new Label(text); l.setStyle("-fx-text-fill:#92a8b5;-fx-font-size:13px;"); l.setWrapText(true); return l; }
    private static Label accent(String text) { var l = title(text, 14); l.setStyle(l.getStyle() + "-fx-text-fill:#22c6da;"); return l; }
}
