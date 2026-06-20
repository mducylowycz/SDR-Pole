package app.sdrpole.desktop;

import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.JmbeInstaller;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Human-driven slippy map that fetches only visible OSM tiles and caches them for at least seven days. */
final class SiteMapView extends Pane {
    private static final int TILE = 256;
    private static final Duration CACHE_AGE = Duration.ofDays(7);
    private static final String USER_AGENT = "SDR-Pole/0.2 (+https://github.com/mducylowycz/SDR-Pole)";
    private final Canvas canvas = new Canvas();
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Path cache = JmbeInstaller.applicationDataDirectory().resolve("map-cache/osm");
    private final Map<TileKey, Image> images = new ConcurrentHashMap<>();
    private final Set<TileKey> loading = ConcurrentHashMap.newKeySet();
    private final ArrayList<SiteMarker> markers = new ArrayList<>();
    private double centerLat = 39.5;
    private double centerLon = -98.35;
    private int zoom = 4;
    private double dragX;
    private double dragY;

    SiteMapView() {
        getChildren().add(canvas);
        setMinHeight(430);
        setPrefHeight(520);
        widthProperty().addListener((o, old, value) -> resize());
        heightProperty().addListener((o, old, value) -> resize());
        addEventHandler(MouseEvent.MOUSE_PRESSED, event -> { dragX = event.getX(); dragY = event.getY(); });
        addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            pan(dragX - event.getX(), dragY - event.getY());
            dragX = event.getX(); dragY = event.getY();
        });
        setOnScroll(event -> {
            int next = Math.max(2, Math.min(18, zoom + (event.getDeltaY() > 0 ? 1 : -1)));
            if (next != zoom) { zoom = next; draw(); }
            event.consume();
        });
    }

    void centerOn(GeoPoint point, int requestedZoom) {
        centerLat = clampLatitude(point.latitude());
        centerLon = point.longitude();
        zoom = Math.max(2, Math.min(18, requestedZoom));
        draw();
    }

    void addMarker(SiteMarker marker) {
        markers.removeIf(existing -> existing.label().equalsIgnoreCase(marker.label()));
        markers.add(marker);
        draw();
    }

    GeoPoint center() { return new GeoPoint(centerLat, centerLon); }

    private void resize() {
        canvas.setWidth(getWidth());
        canvas.setHeight(getHeight());
        draw();
    }

    private void pan(double dx, double dy) {
        double size = TILE * Math.scalb(1.0, zoom);
        double worldX = lonToWorld(centerLon, size) + dx;
        double worldY = latToWorld(centerLat, size) + dy;
        centerLon = worldToLon(worldX, size);
        centerLat = worldToLat(worldY, size);
        draw();
    }

    private void draw() {
        var graphics = canvas.getGraphicsContext2D();
        double width = canvas.getWidth(), height = canvas.getHeight();
        graphics.setFill(Color.web("#09131a"));
        graphics.fillRect(0, 0, width, height);
        if (width < 1 || height < 1) return;
        int count = 1 << zoom;
        double worldSize = TILE * (double) count;
        double left = lonToWorld(centerLon, worldSize) - width / 2;
        double top = latToWorld(centerLat, worldSize) - height / 2;
        int firstX = (int) Math.floor(left / TILE), lastX = (int) Math.floor((left + width) / TILE);
        int firstY = (int) Math.floor(top / TILE), lastY = (int) Math.floor((top + height) / TILE);
        for (int y = firstY; y <= lastY; y++) {
            if (y < 0 || y >= count) continue;
            for (int x = firstX; x <= lastX; x++) {
                int wrappedX = Math.floorMod(x, count);
                var key = new TileKey(zoom, wrappedX, y);
                var image = images.get(key);
                double screenX = x * TILE - left, screenY = y * TILE - top;
                if (image != null) graphics.drawImage(image, screenX, screenY, TILE, TILE);
                else {
                    graphics.setStroke(Color.web("#223641"));
                    graphics.strokeRect(screenX, screenY, TILE, TILE);
                    load(key);
                }
            }
        }
        drawMarkers(graphics, left, top, worldSize);
        graphics.setFill(Color.rgb(5, 12, 16, .82));
        graphics.fillRoundRect(width - 205, height - 28, 197, 22, 6, 6);
        graphics.setFill(Color.WHITE);
        graphics.setFont(Font.font(11));
        graphics.fillText("© OpenStreetMap contributors", width - 198, height - 13);
    }

    private void drawMarkers(javafx.scene.canvas.GraphicsContext graphics, double left, double top, double worldSize) {
        for (var marker : markers) {
            double x = lonToWorld(marker.location().longitude(), worldSize) - left;
            double y = latToWorld(marker.location().latitude(), worldSize) - top;
            graphics.setFill(Color.web("#22c6da"));
            graphics.fillOval(x - 7, y - 7, 14, 14);
            graphics.setStroke(Color.WHITE);
            graphics.strokeOval(x - 7, y - 7, 14, 14);
            graphics.setFill(Color.rgb(4, 12, 16, .88));
            graphics.fillRoundRect(x + 9, y - 13, Math.max(90, marker.label().length() * 7), 24, 6, 6);
            graphics.setFill(Color.WHITE);
            graphics.fillText(marker.label(), x + 14, y + 3);
        }
    }

    private void load(TileKey key) {
        if (!loading.add(key)) return;
        Thread.ofVirtual().start(() -> {
            try {
                var file = cache.resolve(key.zoom() + "/" + key.x() + "/" + key.y() + ".png");
                if (!fresh(file)) download(key, file);
                try (var input = Files.newInputStream(file)) {
                    var image = new Image(input);
                    Platform.runLater(() -> { images.put(key, image); loading.remove(key); draw(); });
                }
            } catch (Exception ignored) {
                loading.remove(key);
            }
        });
    }

    private void download(TileKey key, Path destination) throws IOException, InterruptedException {
        Files.createDirectories(destination.getParent());
        var request = HttpRequest.newBuilder(URI.create("https://tile.openstreetmap.org/%d/%d/%d.png".formatted(key.zoom(), key.x(), key.y())))
                .timeout(Duration.ofSeconds(20)).header("User-Agent", USER_AGENT).GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) throw new IOException("Map tile returned HTTP " + response.statusCode());
        var staging = Files.createTempFile(destination.getParent(), ".tile-", ".png");
        try {
            Files.write(staging, response.body());
            Files.move(staging, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(staging); }
    }

    private static boolean fresh(Path file) throws IOException {
        return Files.isRegularFile(file) && Files.getLastModifiedTime(file).toInstant().plus(CACHE_AGE).isAfter(Instant.now());
    }

    private static double lonToWorld(double lon, double size) { return (lon + 180) / 360 * size; }
    private static double latToWorld(double lat, double size) {
        double radians = Math.toRadians(clampLatitude(lat));
        return (1 - Math.log(Math.tan(radians) + 1 / Math.cos(radians)) / Math.PI) / 2 * size;
    }
    private static double worldToLon(double x, double size) { return ((x % size + size) % size) / size * 360 - 180; }
    private static double worldToLat(double y, double size) {
        double n = Math.PI - 2 * Math.PI * Math.max(0, Math.min(size, y)) / size;
        return Math.toDegrees(Math.atan(Math.sinh(n)));
    }
    private static double clampLatitude(double lat) { return Math.max(-85.05112878, Math.min(85.05112878, lat)); }

    record SiteMarker(String label, GeoPoint location, long controlFrequencyHz) {}
    private record TileKey(int zoom, int x, int y) {}
}
