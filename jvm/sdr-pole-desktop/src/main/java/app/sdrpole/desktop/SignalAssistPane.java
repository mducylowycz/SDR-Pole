package app.sdrpole.desktop;

import app.sdrpole.core.AutoTunePolicy;
import app.sdrpole.core.SignalDetector;
import app.sdrpole.core.SignalObservation;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

/** Owns signal seeking and decoder-suggestion UI; emits tuning intent only. */
final class SignalAssistPane extends VBox {
    private final SignalDetector detector = new SignalDetector();
    private final AutoTunePolicy policy = new AutoTunePolicy(14, 3);
    private final TuningWheel wheel = new TuningWheel();
    private final Slider threshold = new Slider(8, 35, 14);
    private final CheckBox autoTune = new CheckBox("Auto-tune strong signals");
    private final Label hint = new Label("Decoder suggestion: waiting for signal");
    private Consumer<SignalObservation> tuneHandler = ignored -> {};
    private volatile double thresholdDb = 14;

    SignalAssistPane() {
        var step = new ComboBox<Long>();
        step.getItems().addAll(100L, 1_000L, 2_500L, 5_000L, 6_250L, 12_500L, 25_000L, 100_000L);
        step.getSelectionModel().select(6_250L);
        step.valueProperty().addListener((o, old, value) -> wheel.setStepHz(value));
        wheel.setStepHz(step.getValue());
        threshold.setPrefWidth(130);
        var thresholdLabel = muted("14 dB above noise");
        threshold.valueProperty().addListener((o, old, value) -> {
            thresholdDb = value.doubleValue();
            thresholdLabel.setText(Math.round(value.doubleValue()) + " dB above noise");
        });
        autoTune.setStyle("-fx-text-fill: #edf7fa;");
        autoTune.setTooltip(new Tooltip("Locks only after a peak remains above the noise floor for three measurements."));
        hint.setStyle("-fx-text-fill: #22c6da; -fx-font-weight: bold; -fx-font-size: 14px;");
        var row = new HBox(12, field("Tuning wheel", wheel), field("Step", step),
                field("Signal threshold", new VBox(2, threshold, thresholdLabel)), autoTune);
        row.setAlignment(Pos.BOTTOM_LEFT);
        setSpacing(8); getChildren().addAll(row, hint);
    }

    void setOnTuneDelta(LongConsumer handler) { wheel.setOnDelta(handler); }
    void setOnAutoTune(Consumer<SignalObservation> handler) { tuneHandler = handler == null ? ignored -> {} : handler; }
    void setAutoTuneEnabled(boolean enabled) { autoTune.setSelected(enabled); }

    void analyze(float[] powerDb, long centerHz, int sampleRate) {
        var observations = detector.detect(powerDb, centerHz, sampleRate, thresholdDb);
        Platform.runLater(() -> {
            if (observations.isEmpty()) hint.setText("Decoder suggestion: no signal above threshold");
            else {
                var best = observations.getFirst();
                hint.setText(String.format("Likely %s • %.0f dB SNR%s", best.hint().label(), best.snrDb(),
                        best.hint().needsFrameConfirmation() ? " • needs frame confirmation" : ""));
                if (autoTune.isSelected()) policy.consider(observations, Instant.now()).ifPresent(tuneHandler);
            }
        });
    }

    private static VBox field(String title, javafx.scene.Node node) { return new VBox(3, muted(title), node); }
    private static Label muted(String text) { var label = new Label(text); label.setStyle("-fx-text-fill: #92a8b5; -fx-font-size: 13px;"); return label; }
}
