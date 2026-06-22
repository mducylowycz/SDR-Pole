package app.sdrpole.desktop;

import app.sdrpole.core.AudioOutputService;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Repair-oriented setup surface with an immediately verifiable audio route. */
final class SetupPane extends VBox {
    SetupPane(boolean radioReady, String radioDetail, boolean jmbeReady, String selectedAudio,
              Runnable radios, Runnable decoders, Runnable diagnostics,
              Consumer<String> audioSelected, Consumer<String> notice) {
        setSpacing(14);
        var audio = new AudioOutputService();
        var outputs = new ComboBox<AudioOutputService.Output>(); outputs.getItems().setAll(audio.outputs()); outputs.setPrefWidth(300);
        outputs.getSelectionModel().select(outputs.getItems().stream().filter(item -> item.id().equals(selectedAudio)).findFirst()
                .orElse(outputs.getItems().isEmpty() ? null : outputs.getItems().getFirst()));
        var testState = muted(outputs.getItems().isEmpty() ? "No compatible 48 kHz mono output found" : "Choose where live audio should play, then test it.");
        outputs.valueProperty().addListener((o, old, value) -> { if (value != null) audioSelected.accept(value.id()); });
        var test = secondary("Play test tone"); test.setDisable(outputs.getItems().isEmpty());
        test.setOnAction(event -> {
            var selected = outputs.getValue(); if (selected == null) return;
            test.setDisable(true); testState.setText("Playing a short test tone…");
            Thread.ofVirtual().start(() -> {
                try {
                    audio.playTestTone(selected.id());
                    Platform.runLater(() -> { testState.setText("✓ Audio test completed on " + selected.label()); notice.accept("Audio output verified"); });
                } catch (Exception problem) {
                    Platform.runLater(() -> { testState.setText("Audio test failed: " + problem.getMessage()); notice.accept("Choose another audio output and retry"); });
                } finally { Platform.runLater(() -> test.setDisable(false)); }
            });
        });
        var radioButton = primary("Radios"); radioButton.setOnAction(e -> radios.run());
        var decoderButton = primary("Decoder packages"); decoderButton.setOnAction(e -> decoders.run());
        var diagnosticButton = primary("Run diagnostics"); diagnosticButton.setOnAction(e -> diagnostics.run());
        getChildren().addAll(title("Setup & Diagnostics", 30), muted("Every setup item includes a visible result; listening pages stay uncluttered."),
                readiness("Radio", radioReady, radioDetail),
                readiness("Audio", !outputs.getItems().isEmpty(), outputs.getItems().size() + " compatible output(s)"),
                new VBox(7, title("Audio output", 17), new HBox(8, outputs, test), testState),
                readiness("Voice library", jmbeReady, jmbeReady ? "JMBE installed" : "Install JMBE for supported digital voice"),
                new HBox(10, radioButton, decoderButton, diagnosticButton));
    }

    private static HBox readiness(String name, boolean ready, String detail) {
        var icon = title(ready ? "✓" : "○", 18); icon.setStyle(icon.getStyle() + "-fx-text-fill:" + (ready ? "#63e6a5" : "#ffb86b") + ";");
        var spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        var row = new HBox(10, icon, title(name, 15), spacer, muted(detail)); row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color:#14202a;-fx-padding:12 14;-fx-background-radius:12;-fx-border-color:#263b48;-fx-border-radius:12;"); return row;
    }
    private static Button primary(String text) { var b = new Button(text); b.setStyle("-fx-background-color:#22c6da;-fx-text-fill:#061418;-fx-font-weight:bold;-fx-padding:10 16;-fx-background-radius:8;"); return b; }
    private static Button secondary(String text) { var b = new Button(text); b.setStyle("-fx-background-color:#20313d;-fx-text-fill:#edf7fa;-fx-padding:9 14;-fx-background-radius:8;"); return b; }
    private static Label title(String text, int size) { var l = new Label(text); l.setWrapText(true); l.setStyle("-fx-text-fill:#edf7fa;-fx-font-size:" + size + "px;-fx-font-weight:bold;"); return l; }
    private static Label muted(String text) { var l = new Label(text); l.setWrapText(true); l.setStyle("-fx-text-fill:#92a8b5;-fx-font-size:13px;"); return l; }
}
