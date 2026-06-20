package app.sdrpole.desktop;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;

final class WaterfallView extends StackPane {
    private final int width;
    private final int height;
    private final int[] pixels;
    private final WritableImage image;

    WaterfallView(int width, int height) {
        this.width = width;
        this.height = height;
        this.pixels = new int[width * height];
        this.image = new WritableImage(width, height);
        var view = new ImageView(image);
        view.setPreserveRatio(false);
        view.fitWidthProperty().bind(widthProperty());
        view.fitHeightProperty().bind(heightProperty());
        setMinHeight(height);
        setPrefHeight(height);
        setStyle("-fx-background-color: #03070b; -fx-border-color: #27404e;");
        getChildren().add(view);
    }

    void accept(float[] powerDb) {
        var copy = powerDb.clone();
        Platform.runLater(() -> draw(copy));
    }

    private void draw(float[] powerDb) {
        System.arraycopy(pixels, 0, pixels, width, width * (height - 1));
        for (int x = 0; x < width; x++) {
            int bin = Math.min(powerDb.length - 1, x * powerDb.length / width);
            pixels[x] = color(powerDb[bin]);
        }
        image.getPixelWriter().setPixels(0, 0, width, height,
                PixelFormat.getIntArgbPreInstance(), pixels, 0, width);
    }

    private static int color(float db) {
        double n = Math.max(0, Math.min(1, (db + 115) / 75.0));
        int r, g, b;
        if (n < .33) {
            r = 5; g = (int) (n * 3 * 100); b = 35 + (int) (n * 3 * 170);
        } else if (n < .66) {
            var p = (n - .33) * 3;
            r = (int) (p * 245); g = 100 + (int) (p * 100); b = 205 - (int) (p * 170);
        } else {
            var p = (n - .66) * 3;
            r = 245; g = 200 + (int) (p * 55); b = 35 + (int) (p * 220);
        }
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }
}
