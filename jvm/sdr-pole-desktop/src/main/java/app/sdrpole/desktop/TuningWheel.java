package app.sdrpole.desktop;

import javafx.scene.canvas.Canvas;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.MouseEvent;
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
    private double dragAnchor;
    private long emittedDragSteps;

    TuningWheel() {
        setFocusTraversable(true);
        setAccessibleText("Virtual tuning wheel");
        setAccessibleHelp("Scroll, drag, click, or use arrows. Hold Shift for ten times faster tuning and Alt for fine tuning.");
        getChildren().add(canvas); setPrefSize(190, 64);
        draw(false);
        addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) return;
            long direction = event.getDeltaY() > 0 ? 1 : -1;
            long notches = Math.max(1, Math.min(25, Math.round(Math.abs(event.getDeltaY()) / 24.0)));
            handler.accept(direction * adjustedStep(event.isShiftDown(), event.isAltDown()) * notches);
            draw(true); event.consume();
        });
        setOnMouseClicked(event -> handler.accept((event.getX() >= getWidth() / 2 ? 1 : -1)
                * adjustedStep(event.isShiftDown() || event.getClickCount() > 1, event.isAltDown())));
        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> { dragAnchor = event.getX(); emittedDragSteps = 0; requestFocus(); });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            long dragSteps = Math.round((event.getX() - dragAnchor) / 7.0);
            long delta = dragSteps - emittedDragSteps;
            if (delta != 0) { handler.accept(delta * adjustedStep(event.isShiftDown(), event.isAltDown())); emittedDragSteps = dragSteps; draw(true); }
        });
        setOnMouseEntered(e -> draw(true)); setOnMouseExited(e -> draw(false));
        setOnKeyPressed(event -> {
            long amount = adjustedStep(event.isShiftDown(), event.isAltDown());
            if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.DOWN) handler.accept(-amount);
            else if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.UP) handler.accept(amount);
            else if (event.getCode() == KeyCode.PAGE_DOWN) handler.accept(-amount * 10);
            else if (event.getCode() == KeyCode.PAGE_UP) handler.accept(amount * 10);
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
        g.fillText("◀  drag • scroll • click  ▶", 17, 45);
        g.setFill(Color.web("#92a8b5")); g.fillText(formatStep(), 74, 59);
    }

    private String formatStep() { return stepHz >= 1000 ? String.format("%.3f kHz", stepHz / 1000.0) : stepHz + " Hz"; }
    private long adjustedStep(boolean fast, boolean fine) {
        long adjusted = fast ? Math.multiplyExact(stepHz, 10) : stepHz;
        return fine ? Math.max(1, adjusted / 10) : adjusted;
    }
}
