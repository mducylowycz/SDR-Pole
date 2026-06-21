package app.sdrpole.desktop;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import java.util.function.LongConsumer;

/** Combined spectrum and waterfall with calibrated scale, cursor and click tuning. */
final class WaterfallView extends Pane {
    private final Canvas canvas = new Canvas();
    private WritableImage history;
    private int[] pixels;
    private float[] latest = new float[0];
    private long centerHz;
    private int sampleRate = 2_000_000;
    private LongConsumer tuneHandler = ignored -> {};

    WaterfallView(int width, int height) {
        setMinHeight(height); setPrefHeight(height); setPrefWidth(width);
        canvas.widthProperty().bind(widthProperty()); canvas.heightProperty().bind(heightProperty());
        getChildren().add(canvas);
        widthProperty().addListener((o, a, b) -> redraw());
        heightProperty().addListener((o, a, b) -> redraw());
        setOnMouseMoved(e -> draw(e.getX()));
        setOnMouseExited(e -> draw(Double.NaN));
        setOnMouseClicked(e -> tuneHandler.accept(frequencyAt(e.getX())));
        setStyle("-fx-background-color: #03070b; -fx-border-color: #27404e; -fx-border-radius: 8;");
    }

    void setTuning(long centerHz, int sampleRate) { this.centerHz = centerHz; this.sampleRate = sampleRate; redraw(); }
    void setOnTune(LongConsumer handler) { tuneHandler = handler == null ? ignored -> {} : handler; }

    void accept(float[] powerDb) {
        var copy = powerDb.clone();
        Platform.runLater(() -> { latest = copy; appendHistory(); draw(Double.NaN); });
    }

    private void ensureHistory(int width, int height) {
        int w = Math.max(1, width), h = Math.max(1, height);
        if (history != null && (int) history.getWidth() == w && (int) history.getHeight() == h) return;
        history = new WritableImage(w, h); pixels = new int[w * h];
    }

    private void appendHistory() {
        int w = Math.max(1, (int) getWidth()), h = Math.max(1, (int) getHeight() - 115);
        ensureHistory(w, h);
        System.arraycopy(pixels, 0, pixels, w, w * (h - 1));
        for (int x = 0; x < w; x++) {
            int bin = Math.min(latest.length - 1, x * latest.length / w);
            pixels[x] = color(latest[bin]);
        }
        history.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbPreInstance(), pixels, 0, w);
    }

    private void redraw() { Platform.runLater(() -> draw(Double.NaN)); }

    private void draw(double cursorX) {
        double w = getWidth(), h = getHeight(), spectrumH = 105;
        if (w <= 2 || h <= spectrumH) return;
        var g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#03070b")); g.fillRect(0, 0, w, h);
        g.setStroke(Color.web("#17303b")); g.setLineWidth(1);
        for (int i = 0; i <= 4; i++) { double y = 8 + i * 22; g.strokeLine(0, y, w, y); }
        for (int i = 0; i <= 8; i++) { double x = i * w / 8; g.strokeLine(x, 0, x, h); }
        if (latest.length > 1) {
            g.setStroke(Color.web("#43e5ee")); g.setLineWidth(1.5); g.beginPath();
            for (int x = 0; x < (int) w; x++) {
                int bin = Math.min(latest.length - 1, x * latest.length / (int) w);
                double y = 8 + Math.max(0, Math.min(88, (-latest[bin] - 30) * 88 / 100));
                if (x == 0) g.moveTo(x, y); else g.lineTo(x, y);
            }
            g.stroke();
        }
        if (history != null) g.drawImage(history, 0, spectrumH, w, h - spectrumH);
        g.setFont(Font.font(11)); g.setFill(Color.web("#90a8b4"));
        g.fillText("−30 dB", 6, 15); g.fillText("−80", 6, 58); g.fillText("−130", 6, 99);
        for (int i = 0; i <= 4; i++) {
            long hz = centerHz - sampleRate / 2L + sampleRate * i / 4L;
            g.fillText(String.format("%.3f", hz / 1e6), Math.max(4, i * w / 4 - 24), h - 6);
        }
        g.setStroke(Color.web("#ffce54")); g.setLineWidth(1.2); g.strokeLine(w / 2, 0, w / 2, h);
        if (!Double.isNaN(cursorX)) {
            g.setStroke(Color.web("#ffffff88")); g.strokeLine(cursorX, 0, cursorX, h);
            g.setFill(Color.WHITE); g.fillText(String.format("%.5f MHz", frequencyAt(cursorX) / 1e6), Math.min(w - 95, cursorX + 5), 15);
        }
    }

    private long frequencyAt(double x) { return Math.round(centerHz + (x / Math.max(1, getWidth()) - .5) * sampleRate); }

    private static int color(float db) {
        double n = Math.max(0, Math.min(1, (db + 120) / 85.0));
        double r = Math.max(0, Math.min(1, 1.5 * n - .45));
        double g = Math.max(0, Math.min(1, 1.8 * n - .15));
        double b = Math.max(0, Math.min(1, 1.2 - 1.25 * n + .25));
        int ri = (int) (255 * r), gi = (int) (255 * g), bi = (int) (255 * b);
        return 0xff000000 | (ri << 16) | (gi << 8) | bi;
    }
}
