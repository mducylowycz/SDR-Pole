package app.sdrpole.core.p25;

public sealed interface P25ControlEvent permits P25ControlEvent.NetworkStatus,
        P25ControlEvent.IdentifierUpdate, P25ControlEvent.GroupVoiceGrant, P25ControlEvent.AdjacentSite {

    record NetworkStatus(P25SystemIdentity identity, int rfss, int site, int channelIdentifier,
                         int channelNumber) implements P25ControlEvent {}

    record IdentifierUpdate(P25BandPlan bandPlan) implements P25ControlEvent {}

    record GroupVoiceGrant(int talkgroup, int sourceRadio, int channelIdentifier,
                           int channelNumber, int slot, boolean encrypted) implements P25ControlEvent {
        public GroupVoiceGrant {
            if (talkgroup < 0 || talkgroup > 0xFFFF) throw new IllegalArgumentException("Talkgroup must be 16-bit");
            if (sourceRadio < 0 || sourceRadio > 0xFFFFFF) throw new IllegalArgumentException("Radio ID must be 24-bit");
            if (slot < 0 || slot > 1) throw new IllegalArgumentException("TDMA slot must be 0 or 1");
        }
    }

    record AdjacentSite(int rfss, int site, int channelIdentifier, int channelNumber) implements P25ControlEvent {}
}
