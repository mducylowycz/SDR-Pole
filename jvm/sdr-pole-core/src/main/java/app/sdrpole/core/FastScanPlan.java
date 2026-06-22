package app.sdrpole.core;

import java.util.ArrayList;
import java.util.List;

/** FFT-window sweep plan: covers bands by usable receiver bandwidth instead of slow channel-by-channel hopping. */
public final class FastScanPlan {
    private static final double USABLE_BANDWIDTH = .72;
    private static final double WINDOW_OVERLAP = .12;
    private final List<Window> windows;
    private int index;

    public FastScanPlan(List<ScanRange> ranges, int sampleRate) {
        if (ranges == null || ranges.isEmpty()) throw new IllegalArgumentException("A fast scan needs at least one range");
        if (sampleRate < 250_000) throw new IllegalArgumentException("Sample rate is too low for fast scanning");
        var result = new ArrayList<Window>();
        long usable = Math.max(100_000, Math.round(sampleRate * USABLE_BANDWIDTH));
        long advance = Math.max(50_000, Math.round(usable * (1 - WINDOW_OVERLAP)));
        for (int rangeIndex = 0; rangeIndex < ranges.size(); rangeIndex++) {
            var range = ranges.get(rangeIndex);
            if (range.startHz() == range.endHz() || range.endHz() - range.startHz() <= usable) {
                result.add(new Window(range.startHz() + (range.endHz() - range.startHz()) / 2,
                        range.mode(), rangeIndex + 1, ranges.size()));
                continue;
            }
            long half = usable / 2;
            for (long center = range.startHz() + half; center < range.endHz(); center += advance) {
                long clamped = Math.min(center, range.endHz() - half);
                var window = new Window(clamped, range.mode(), rangeIndex + 1, ranges.size());
                if (result.isEmpty() || result.getLast().centerFrequencyHz() != clamped) result.add(window);
                if (clamped == range.endHz() - half) break;
            }
        }
        windows = List.copyOf(result);
    }

    public synchronized Window next() { var result = windows.get(index); index = (index + 1) % windows.size(); return result; }
    public int windowCount() { return windows.size(); }

    public record Window(long centerFrequencyHz, DemodulationMode mode, int rangeNumber, int rangeCount) {}
}
