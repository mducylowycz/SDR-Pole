package app.sdrpole.core.directory;

import app.sdrpole.core.p25.P25SystemConfig;
import app.sdrpole.core.p25.P25Talkgroup;

import java.time.Instant;
import java.util.List;

public record DirectoryUpdate(String provider, String areaLabel, Instant retrievedAt,
                              List<P25SystemConfig> p25Sites, List<P25Talkgroup> talkgroups) {
    public DirectoryUpdate {
        p25Sites = List.copyOf(p25Sites);
        talkgroups = List.copyOf(talkgroups);
    }
}
