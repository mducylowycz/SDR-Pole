package app.sdrpole.core;

import app.sdrpole.plugin.DecoderCapability;
import java.net.URI;
import java.util.List;
import java.util.Set;

/** Curated catalog. Downloads are enabled only after a versioned, signed artifact is published. */
public final class BuiltInDecoderCatalog {
    private BuiltInDecoderCatalog() {}

    public static List<DecoderCatalogEntry> entries() {
        return List.of(
                new DecoderCatalogEntry("analog-nfm", "Analog FM / NFM",
                        "Conventional voice, weather, amateur, and public-safety channels.",
                        "GPL-3.0-or-later", URI.create("https://github.com/mducylowycz/SDR-Pole"),
                        Set.of(DecoderCapability.ANALOG_FM), DecoderCatalogEntry.Availability.BUILT_IN),
                new DecoderCatalogEntry("p25-phase1", "P25 Phase 1",
                        "C4FM/CQPSK framing, trunk control channels, talkgroups, and IMBE voice.",
                        "Separate decoder package", URI.create("https://github.com/boatbod/op25"),
                        Set.of(DecoderCapability.P25_PHASE_1, DecoderCapability.TRUNK_FOLLOWING),
                        DecoderCatalogEntry.Availability.REQUIRES_PACKAGE),
                new DecoderCatalogEntry("p25-phase2", "P25 Phase 2",
                        "TDMA traffic channels, talkgroups, and AMBE+2 voice where legally available.",
                        "Separate decoder/vocoder terms", URI.create("https://github.com/boatbod/op25"),
                        Set.of(DecoderCapability.P25_PHASE_2, DecoderCapability.TRUNK_FOLLOWING),
                        DecoderCatalogEntry.Availability.REQUIRES_PACKAGE),
                new DecoderCatalogEntry("dmr", "DMR",
                        "Conventional and trunked DMR with color codes and talkgroups.",
                        "Planned package", URI.create("https://github.com/f4exb/dsdcc"),
                        Set.of(DecoderCapability.DMR), DecoderCatalogEntry.Availability.COMING_SOON),
                new DecoderCatalogEntry("nxdn", "NXDN",
                        "NXDN48/NXDN96 conventional and trunked systems.",
                        "Planned package", URI.create("https://github.com/f4exb/dsdcc"),
                        Set.of(DecoderCapability.NXDN), DecoderCatalogEntry.Availability.COMING_SOON));
    }
}
