package app.sdrpole.desktop;

import app.sdrpole.core.FrequencyBandCatalog;
import app.sdrpole.core.directory.FrequencySourceCatalog;
import app.sdrpole.core.directory.FrequencySourceDescriptor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Map;

/** Read-only source/library presentation. Downloaders and persistence deliberately live elsewhere. */
final class FrequencyLibraryPane extends VBox {
    FrequencyLibraryPane(Map<String, Integer> installedCounts) {
        setSpacing(14);
        var sources = new VBox(8);
        FrequencySourceCatalog.all().forEach(source -> sources.getChildren().add(sourceRow(source, installedCounts.getOrDefault(source.id(), 0))));
        var bands = new VBox(7);
        FrequencyBandCatalog.northAmerica().forEach(band -> bands.getChildren().add(row(band.name(),
                band.rangeLabel() + " • " + band.mode() + " • " + band.commonUse(), "BUNDLED")));
        getChildren().addAll(title("Frequency Library", 30),
                muted("Every result keeps its provider and evidence level. Official licences, directory labels, and live measurements are complementary—not interchangeable."),
                title("Data sources", 19), sources, title("Installed range guide", 19), bands);
    }

    private static HBox sourceRow(FrequencySourceDescriptor source, int count) {
        var detail = source.coverage() + " • " + source.refresh() + " • " + source.limitation();
        var state = count > 0 ? count + " LOCAL" : switch (source.availability()) {
            case READY -> "READY"; case IMPORTER_NEXT -> "NEXT"; case OPTIONAL -> "OPTIONAL";
        };
        return row(source.name(), detail, state);
    }

    private static HBox row(String name, String detail, String state) {
        var copy = new VBox(2, title(name, 15), muted(detail)); HBox.setHgrow(copy, Priority.ALWAYS);
        var result = new HBox(10, copy, accent(state)); result.setAlignment(Pos.CENTER_LEFT); result.setPadding(new Insets(11));
        result.setStyle("-fx-background-color:#14202a;-fx-background-radius:10;-fx-border-color:#263b48;-fx-border-radius:10;");
        return result;
    }

    private static Label title(String text, int size) { var label = new Label(text); label.setWrapText(true); label.setStyle("-fx-text-fill:#edf7fa;-fx-font-size:" + size + "px;-fx-font-weight:bold;"); return label; }
    private static Label muted(String text) { var label = new Label(text); label.setWrapText(true); label.setStyle("-fx-text-fill:#92a8b5;-fx-font-size:13px;"); return label; }
    private static Label accent(String text) { var label = title(text, 13); label.setStyle(label.getStyle() + "-fx-text-fill:#22c6da;"); return label; }
}
