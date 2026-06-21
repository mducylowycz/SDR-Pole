package app.sdrpole.desktop;

import javafx.scene.canvas.Canvas;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.function.LongConsumer;
import javafx.scene.input.KeyCode;

/** Mouse/trackpad tuning wheel with selectable step size. */
final class TuningWheel extends StackPane {
    private final Canvas canvas = new Canvas(190, 64);
    private long stepHz = 12_500;
    private LongConsumer handler = ignored -> {};

    TuningWheel() {
        setFocusTraversable(true);
        setAccessibleText("Virtual tuning wheel");
        setAccessibleHelp("Use left and right arrow keys, mouse click, or scroll to tune by the selected step.");
        getChildren().add(canvas); setPrefSize(190, 64);
        draw(false);
        addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) return;
            handler.accept(event.getDeltaY() > 0 ? stepHz : -stepHz);
            draw(true); event.consume();
        });
        setOnMouseClicked(event -> handler.accept(event.getX() >= getWidth() / 2 ? stepHz : -stepHz));
        setOnMouseEntered(e -> draw(true)); setOnMouseExited(e -> draw(false));
        setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.DOWN) handler.accept(-stepHz);
            else if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.UP) handler.accept(stepHz);
            else return;
            event.consume();
        });
    }

    void setStepHz(long stepHz) { this.stepHz = Math.max(1, stepHz); draw(false); }
    void setOnDelta(LongConsumer handler) { this.handler = handler == null ? ignored -> {} : handler; }

    private void draw(boolean active) {
        var g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#15242e")); g.fillRoundRect(0, 0, 190, 64, 14, 14);
        g.setStroke(Color.web(active ? "#22c6da" : "#35505e")); g.setLineWidth(2); g.strokeRoundRect(1, 1, 188, 62, 14, 14);
        g.setStroke(Color.web("#7e98a5"));
        for (int x = 46; x <= 144; x += 14) g.strokeLine(x, 11, x, 27);
        g.setFill(Color.web("#edf7fa")); g.setFont(Font.font(12));
        g.fillText("◀  scroll / click  ▶", 28, 45);
        g.setFill(Color.web("#92a8b5")); g.fillText(formatStep(), 74, 59);
    }

    private String formatStep() { return stepHz >= 1000 ? String.format("%.3f kHz", stepHz / 1000.0) : stepHz + " Hz"; }
}
