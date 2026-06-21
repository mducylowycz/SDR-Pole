package app.sdrpole.core.directory;

import java.net.URI;
import java.util.Set;

/** User-visible policy and capability metadata for an independently replaceable directory adapter. */
public record FrequencySourceDescriptor(String id, String name, String coverage, Access access,
                                        URI homepage, Set<Capability> capabilities, String refresh,
                                        Availability availability, String limitation) {
    public enum Access { OPEN_DOWNLOAD, ACCOUNT, LOCAL_RADIO }
    public enum Capability { ALLOCATIONS, LICENSES, SITES, EXACT_CHANNELS, TRUNKING, TALKGROUPS, ACTIVITY }
    public enum Availability { READY, IMPORTER_NEXT, OPTIONAL }

    public FrequencySourceDescriptor {
        capabilities = Set.copyOf(capabilities);
    }
}
