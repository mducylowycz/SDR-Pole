package app.sdrpole.core;

import java.util.ArrayList;
import java.util.List;

/** Cycles one or more named ranges without putting scheduling state in the UI shell. */
public final class ScanPlan {
    private final List<ScanRange> ranges;
    private int rangeIndex;
    private long frequency;

    public ScanPlan(List<ScanRange> ranges) {
        if (ranges == null || ranges.isEmpty()) throw new IllegalArgumentException("A scan plan needs at least one range");
        this.ranges = List.copyOf(ranges); this.frequency = ranges.getFirst().startHz();
    }

    public synchronized Step next() {
        var range = ranges.get(rangeIndex);
        var result = new Step(frequency, range.mode(), rangeIndex + 1, ranges.size());
        frequency += range.stepHz();
        if (frequency > range.endHz()) {
            rangeIndex = (rangeIndex + 1) % ranges.size();
            frequency = ranges.get(rangeIndex).startHz();
        }
        return result;
    }

    public static String encode(List<ScanRange> ranges) {
        return String.join(";", ranges.stream().map(r -> r.startHz() + ":" + r.endHz() + ":" + r.stepHz() + ":" + r.mode().name()).toList());
    }

    public static List<ScanRange> decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return List.of();
        var result = new ArrayList<ScanRange>();
        for (var item : encoded.split(";")) {
            try {
                var parts = item.split(":");
                result.add(new ScanRange(Long.parseLong(parts[0]), Long.parseLong(parts[1]), Long.parseLong(parts[2]), DemodulationMode.valueOf(parts[3])));
            } catch (RuntimeException ignored) { }
        }
        return List.copyOf(result);
    }

    public record Step(long frequencyHz, DemodulationMode mode, int rangeNumber, int rangeCount) {}
}
