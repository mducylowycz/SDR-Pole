package app.sdrpole.core.directory;

import app.sdrpole.core.DemodulationMode;
import app.sdrpole.core.GeoPoint;
import app.sdrpole.core.JmbeInstaller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small SQLite repository. Schema migration and SQL stay out of UI/controllers and provider adapters. */
public final class FrequencyDatabase {
    private static final int SCHEMA_VERSION = 1;
    private final Path file;

    public FrequencyDatabase() { this(JmbeInstaller.applicationDataDirectory().resolve("directory/frequencies.sqlite3")); }
    public FrequencyDatabase(Path file) { this.file = file.toAbsolutePath().normalize(); }

    public void initialize() throws Exception {
        Files.createDirectories(file.getParent());
        try (var connection = open(); var statement = connection.createStatement()) {
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA foreign_keys=ON");
            int version;
            try (var versions = statement.executeQuery("PRAGMA user_version")) {
                if (!versions.next()) throw new SQLException("SQLite did not return a schema version");
                version = versions.getInt(1);
            }
            if (version > SCHEMA_VERSION) throw new SQLException("Frequency database was created by a newer SDR-Pole version");
            if (version == 0) {
                statement.executeUpdate("""
                        CREATE TABLE frequency_channel (
                          source_id TEXT NOT NULL, external_id TEXT NOT NULL, name TEXT NOT NULL,
                          frequency_hz INTEGER NOT NULL CHECK(frequency_hz > 0), mode TEXT NOT NULL,
                          common_use TEXT NOT NULL, region TEXT NOT NULL, latitude REAL, longitude REAL,
                          location_accuracy_km REAL NOT NULL DEFAULT 0, confidence TEXT NOT NULL,
                          updated_at TEXT NOT NULL, PRIMARY KEY(source_id, external_id))
                        """);
                statement.executeUpdate("CREATE INDEX frequency_channel_hz ON frequency_channel(frequency_hz)");
                statement.executeUpdate("CREATE INDEX frequency_channel_location ON frequency_channel(latitude, longitude)");
                statement.executeUpdate("PRAGMA user_version=" + SCHEMA_VERSION);
            }
        }
        seedOfficialReferenceChannels();
    }

    public void upsert(List<FrequencyChannel> channels) throws SQLException {
        try (var connection = open(); var statement = connection.prepareStatement("""
                INSERT INTO frequency_channel(source_id, external_id, name, frequency_hz, mode, common_use, region,
                  latitude, longitude, location_accuracy_km, confidence, updated_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(source_id, external_id) DO UPDATE SET name=excluded.name, frequency_hz=excluded.frequency_hz,
                  mode=excluded.mode, common_use=excluded.common_use, region=excluded.region, latitude=excluded.latitude,
                  longitude=excluded.longitude, location_accuracy_km=excluded.location_accuracy_km,
                  confidence=excluded.confidence, updated_at=excluded.updated_at
                """)) {
            connection.setAutoCommit(false);
            try {
                for (var channel : channels) {
                    statement.setString(1, channel.sourceId()); statement.setString(2, channel.externalId());
                    statement.setString(3, channel.name()); statement.setLong(4, channel.frequencyHz());
                    statement.setString(5, channel.mode().name()); statement.setString(6, channel.commonUse());
                    statement.setString(7, channel.region());
                    if (channel.location() == null) { statement.setNull(8, java.sql.Types.REAL); statement.setNull(9, java.sql.Types.REAL); }
                    else { statement.setDouble(8, channel.location().latitude()); statement.setDouble(9, channel.location().longitude()); }
                    statement.setDouble(10, channel.locationAccuracyKm()); statement.setString(11, channel.confidence().name());
                    statement.setString(12, channel.updatedAt().toString()); statement.addBatch();
                }
                statement.executeBatch(); connection.commit();
            } catch (SQLException error) { connection.rollback(); throw error; }
        }
    }

    public List<FrequencyChannel> channelsNear(GeoPoint point, double radiusKm, int limit) throws SQLException {
        if (radiusKm <= 0 || limit <= 0) throw new IllegalArgumentException("Radius and limit must be positive");
        var latitudeWindow = radiusKm / 111.0;
        var longitudeWindow = radiusKm / Math.max(1, 111.0 * Math.cos(Math.toRadians(point.latitude())));
        var candidates = new ArrayList<FrequencyChannel>();
        try (var connection = open(); var statement = connection.prepareStatement("""
                SELECT * FROM frequency_channel WHERE latitude IS NULL OR
                  (latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?)
                ORDER BY frequency_hz LIMIT ?
                """)) {
            statement.setDouble(1, point.latitude() - latitudeWindow); statement.setDouble(2, point.latitude() + latitudeWindow);
            statement.setDouble(3, point.longitude() - longitudeWindow); statement.setDouble(4, point.longitude() + longitudeWindow);
            statement.setInt(5, Math.max(limit * 4, limit));
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    var channel = read(rows);
                    if (channel.location() == null || point.distanceKmTo(channel.location()) <= radiusKm) candidates.add(channel);
                }
            }
        }
        return candidates.stream().limit(limit).toList();
    }

    public Map<String, Integer> countsBySource() throws SQLException {
        var counts = new LinkedHashMap<String, Integer>();
        try (var connection = open(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT source_id, COUNT(*) AS total FROM frequency_channel GROUP BY source_id ORDER BY source_id")) {
            while (rows.next()) counts.put(rows.getString("source_id"), rows.getInt("total"));
        }
        return Map.copyOf(counts);
    }

    private void seedOfficialReferenceChannels() throws SQLException {
        var frequencies = List.of(162_400_000L, 162_425_000L, 162_450_000L, 162_475_000L,
                162_500_000L, 162_525_000L, 162_550_000L);
        var now = Instant.parse("2026-01-01T00:00:00Z");
        upsert(java.util.stream.IntStream.range(0, frequencies.size()).mapToObj(index ->
                new FrequencyChannel("noaa-nwr", "channel-" + (index + 1), "NOAA Weather " + (index + 1),
                        frequencies.get(index), DemodulationMode.NFM, "Continuous weather and hazard broadcasts",
                        "United States", null, 0, FrequencyChannel.Confidence.REFERENCE, now)).toList());
    }

    private java.sql.Connection open() throws SQLException { return DriverManager.getConnection("jdbc:sqlite:" + file); }

    private static FrequencyChannel read(java.sql.ResultSet row) throws SQLException {
        var latitude = row.getObject("latitude") == null ? null : new GeoPoint(row.getDouble("latitude"), row.getDouble("longitude"));
        return new FrequencyChannel(row.getString("source_id"), row.getString("external_id"), row.getString("name"),
                row.getLong("frequency_hz"), DemodulationMode.valueOf(row.getString("mode")), row.getString("common_use"),
                row.getString("region"), latitude, row.getDouble("location_accuracy_km"),
                FrequencyChannel.Confidence.valueOf(row.getString("confidence")), Instant.parse(row.getString("updated_at")));
    }
}
