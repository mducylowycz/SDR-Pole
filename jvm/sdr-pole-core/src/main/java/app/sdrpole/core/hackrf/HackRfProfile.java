package app.sdrpole.core.hackrf;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Immutable inventory and capability assessment from official read-only HackRF tools. */
public record HackRfProfile(String serial, String board, String boardId, String firmware,
                            String firmwareApi, String hostTools, String libraryVersion,
                            String partId, boolean sweepTool, boolean clockTool,
                            boolean operaCakeTool, List<String> findings) {
    public HackRfProfile { findings = List.copyOf(findings); }

    public boolean pro() { return board.toLowerCase().contains("pro"); }
    public long minimumFrequencyHz() { return pro() ? 100_000 : 1_000_000; }
    public long maximumFrequencyHz() { return 6_000_000_000L; }
    public int maximumStandardSampleRate() { return 20_000_000; }
    public int maximumHalfPrecisionSampleRate() { return pro() ? 40_000_000 : 0; }
    public String sampleFormat() { return pro() ? "8-bit IQ; 16-bit extended or 4-bit half precision when supported" : "8-bit signed IQ"; }

    public Map<String, String> properties() {
        var values = new LinkedHashMap<String, String>();
        values.put("hackrf.board", board); values.put("hackrf.boardId", boardId); values.put("hackrf.firmware", firmware);
        values.put("hackrf.api", firmwareApi); values.put("hackrf.hostTools", hostTools); values.put("hackrf.library", libraryVersion);
        values.put("hackrf.partId", partId); values.put("hackrf.sweep", Boolean.toString(sweepTool));
        values.put("hackrf.clock", Boolean.toString(clockTool)); values.put("hackrf.operacake", Boolean.toString(operaCakeTool));
        values.put("hackrf.findings", String.join("\n", findings));
        return Map.copyOf(values);
    }
}
