package app.sdrpole.core.p25;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Resolves decoded P25 control-channel events into a live system/site/call state. */
public final class P25TrunkingState {
    private final Map<Integer, P25BandPlan> bandPlans = new LinkedHashMap<>();
    private final Map<String, NeighborSite> neighbors = new LinkedHashMap<>();
    private final Map<CallKey, ActiveCall> calls = new LinkedHashMap<>();
    private final List<P25ControlEvent.GroupVoiceGrant> pendingGrants = new ArrayList<>();
    private P25SystemIdentity identity;
    private Site currentSite;

    public synchronized void accept(P25ControlEvent event, Instant observedAt) {
        switch (event) {
            case P25ControlEvent.IdentifierUpdate update -> {
                bandPlans.put(update.bandPlan().identifier(), update.bandPlan());
                resolvePending(observedAt);
            }
            case P25ControlEvent.NetworkStatus status -> {
                identity = status.identity();
                currentSite = new Site(status.rfss(), status.site(), resolve(status.channelIdentifier(), status.channelNumber()).orElse(0L));
            }
            case P25ControlEvent.GroupVoiceGrant grant -> {
                if (!addGrant(grant, observedAt)) pendingGrants.add(grant);
            }
            case P25ControlEvent.AdjacentSite adjacent -> {
                long frequency = resolve(adjacent.channelIdentifier(), adjacent.channelNumber()).orElse(0L);
                neighbors.put(key(adjacent.rfss(), adjacent.site()), new NeighborSite(adjacent.rfss(), adjacent.site(), frequency, observedAt));
            }
        }
    }

    public synchronized void expireCalls(Instant now, Duration silence) {
        calls.values().removeIf(call -> call.lastSeen().plus(silence).isBefore(now));
    }

    public synchronized Optional<P25SystemIdentity> identity() { return Optional.ofNullable(identity); }
    public synchronized Optional<Site> currentSite() { return Optional.ofNullable(currentSite); }
    public synchronized List<P25BandPlan> bandPlans() { return List.copyOf(bandPlans.values()); }
    public synchronized List<NeighborSite> neighbors() { return List.copyOf(neighbors.values()); }
    public synchronized List<ActiveCall> activeCalls() { return List.copyOf(calls.values()); }
    public synchronized int unresolvedGrantCount() { return pendingGrants.size(); }

    private boolean addGrant(P25ControlEvent.GroupVoiceGrant grant, Instant observedAt) {
        var frequency = resolve(grant.channelIdentifier(), grant.channelNumber());
        if (frequency.isEmpty()) return false;
        var call = new ActiveCall(grant.talkgroup(), grant.sourceRadio(), frequency.get(), grant.slot(), grant.encrypted(), observedAt);
        calls.put(new CallKey(frequency.get(), grant.slot()), call);
        return true;
    }

    private void resolvePending(Instant observedAt) {
        var iterator = pendingGrants.iterator();
        while (iterator.hasNext()) if (addGrant(iterator.next(), observedAt)) iterator.remove();
    }

    private Optional<Long> resolve(int identifier, int channelNumber) {
        var plan = bandPlans.get(identifier);
        return plan == null ? Optional.empty() : Optional.of(plan.downlinkHz(channelNumber));
    }

    private static String key(int rfss, int site) { return rfss + ":" + site; }

    private record CallKey(long frequencyHz, int slot) {}
    public record Site(int rfss, int site, long controlFrequencyHz) {}
    public record NeighborSite(int rfss, int site, long controlFrequencyHz, Instant lastSeen) {}
    public record ActiveCall(int talkgroup, int sourceRadio, long frequencyHz, int slot,
                             boolean encrypted, Instant lastSeen) {}
}
