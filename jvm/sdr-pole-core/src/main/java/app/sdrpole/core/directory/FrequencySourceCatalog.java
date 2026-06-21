package app.sdrpole.core.directory;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static app.sdrpole.core.directory.FrequencySourceDescriptor.Access.*;
import static app.sdrpole.core.directory.FrequencySourceDescriptor.Availability.*;
import static app.sdrpole.core.directory.FrequencySourceDescriptor.Capability.*;

/** Curated provider catalog. URLs point to the provider, never an unofficial scraper. */
public final class FrequencySourceCatalog {
    private FrequencySourceCatalog() {}

    public static List<FrequencySourceDescriptor> all() {
        return List.of(
                source("noaa-nwr", "NOAA Weather Radio", "United States", OPEN_DOWNLOAD,
                        "https://www.weather.gov/nwr/", Set.of(EXACT_CHANNELS), "Official reference", READY,
                        "Seven nationwide channels; the radio determines which transmitter is active nearby."),
                source("fcc-uls", "FCC Universal Licensing System", "United States", OPEN_DOWNLOAD,
                        "https://www.fcc.gov/wireless/data/public-access-files-database-downloads", Set.of(LICENSES, SITES), "Daily/weekly", IMPORTER_NEXT,
                        "Licensed assignments are candidates, not proof of current activity or a trunking role."),
                source("ised-tafl", "ISED TAFL", "Canada", OPEN_DOWNLOAD,
                        "https://ised-isde.canada.ca/site/spectrum-management-telecommunications/en/spectrum-allocation/technical-and-administrative-frequency-list-tafl", Set.of(LICENSES, SITES), "Public extract", IMPORTER_NEXT,
                        "Authorization records do not identify every active or trunked channel."),
                source("acma-rrl", "ACMA RRL", "Australia", OPEN_DOWNLOAD,
                        "https://www.acma.gov.au/radiocomms-licence-data", Set.of(LICENSES, SITES), "Regenerated daily", IMPORTER_NEXT,
                        "Attribution and personal-information restrictions apply to derivatives."),
                source("ofcom-sis", "Ofcom Spectrum Information", "United Kingdom", OPEN_DOWNLOAD,
                        "https://www.ofcom.org.uk/spectrum/frequencies/spectrum-information-system-sis", Set.of(ALLOCATIONS, LICENSES), "Daily", IMPORTER_NEXT,
                        "Some assignments are represented as areas or bands rather than receiver-ready channels."),
                source("radioreference", "RadioReference", "Multiple countries", ACCOUNT,
                        "https://www.radioreference.com/apps/db/", Set.of(SITES, EXACT_CHANNELS, TRUNKING, TALKGROUPS), "On demand", OPTIONAL,
                        "Requires an approved application key and each user's eligible account."),
                source("local-survey", "Local spectrum survey", "At the receiver", LOCAL_RADIO,
                        "https://github.com/mducylowycz/SDR-Pole", Set.of(EXACT_CHANNELS, ACTIVITY), "Live", READY,
                        "Measured energy is strongest evidence of activity but does not by itself identify the user or protocol."));
    }

    private static FrequencySourceDescriptor source(String id, String name, String coverage,
                                                     FrequencySourceDescriptor.Access access, String url,
                                                     Set<FrequencySourceDescriptor.Capability> capabilities,
                                                     String refresh, FrequencySourceDescriptor.Availability availability,
                                                     String limitation) {
        return new FrequencySourceDescriptor(id, name, coverage, access, URI.create(url), capabilities, refresh, availability, limitation);
    }
}
