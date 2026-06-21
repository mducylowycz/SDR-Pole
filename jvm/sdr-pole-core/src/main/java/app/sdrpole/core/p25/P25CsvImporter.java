package app.sdrpole.core.p25;

import app.sdrpole.core.GeoPoint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Imports plain CSV exports using friendly header aliases instead of requiring one rigid template. */
public final class P25CsvImporter {
    public List<P25SystemConfig> read(Path file) throws IOException {
        var lines = Files.readAllLines(file);
        if (lines.isEmpty()) throw new IOException("The CSV file is empty");
        var headers = split(lines.getFirst());
        var columns = new HashMap<String, Integer>();
        for (int index = 0; index < headers.size(); index++) columns.put(normalize(headers.get(index)), index);
        int systemColumn = required(columns, "system", "systemname", "trunkedsystem");
        int siteColumn = required(columns, "site", "sitename");
        int controlColumn = required(columns, "controlfrequency", "controlfrequencymhz", "controlchannels", "frequencies");
        int latitudeColumn = optional(columns, "latitude", "lat");
        int longitudeColumn = optional(columns, "longitude", "lon", "lng");
        int modulationColumn = optional(columns, "modulation", "simulcast", "type");

        var result = new ArrayList<P25SystemConfig>();
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            if (lines.get(lineNumber).isBlank()) continue;
            var values = split(lines.get(lineNumber));
            try {
                var frequencies = parseFrequencies(value(values, controlColumn));
                GeoPoint location = null;
                var latitude = value(values, latitudeColumn);
                var longitude = value(values, longitudeColumn);
                if (!latitude.isBlank() || !longitude.isBlank()) {
                    if (latitude.isBlank() || longitude.isBlank()) throw new IllegalArgumentException("both coordinates are required");
                    location = new GeoPoint(Double.parseDouble(latitude), Double.parseDouble(longitude));
                }
                var modulation = value(values, modulationColumn).toLowerCase(Locale.ROOT);
                boolean simulcast = modulation.contains("simulcast") || modulation.contains("lsm")
                        || modulation.contains("cqpsk") || modulation.equals("true") || modulation.equals("yes");
                result.add(new P25SystemConfig(value(values, systemColumn), value(values, siteColumn), frequencies,
                        simulcast ? P25SystemConfig.Modulation.LSM_SIMULCAST : P25SystemConfig.Modulation.C4FM,
                        4, location));
            } catch (RuntimeException problem) {
                throw new IOException("CSV row " + (lineNumber + 1) + " is invalid: " + problem.getMessage(), problem);
            }
        }
        if (result.isEmpty()) throw new IOException("The CSV did not contain any P25 sites");
        return List.copyOf(result);
    }

    private static List<Long> parseFrequencies(String input) {
        var result = new ArrayList<Long>();
        for (var token : input.replace("_", "").split("[;| /]+")) {
            if (token.isBlank()) continue;
            double number = Double.parseDouble(token);
            long hz = number < 10_000 ? Math.round(number * 1_000_000) : Math.round(number);
            if (hz < 25_000_000L || hz > 1_750_000_000L) throw new IllegalArgumentException("control frequency is outside the supported range");
            if (!result.contains(hz)) result.add(hz);
        }
        if (result.isEmpty()) throw new IllegalArgumentException("a control frequency is required");
        return List.copyOf(result);
    }

    private static int required(Map<String, Integer> columns, String... aliases) throws IOException {
        int result = optional(columns, aliases);
        if (result < 0) throw new IOException("Required CSV column is missing: " + aliases[0]);
        return result;
    }

    private static int optional(Map<String, Integer> columns, String... aliases) {
        for (var alias : aliases) if (columns.containsKey(alias)) return columns.get(alias);
        return -1;
    }

    private static String value(List<String> values, int index) {
        return index < 0 || index >= values.size() ? "" : values.get(index).trim();
    }

    private static String normalize(String value) { return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""); }

    private static List<String> split(String line) {
        var result = new ArrayList<String>();
        var field = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') { field.append('"'); index++; }
                else quoted = !quoted;
            } else if (character == ',' && !quoted) { result.add(field.toString()); field.setLength(0); }
            else field.append(character);
        }
        result.add(field.toString());
        return result;
    }
}
