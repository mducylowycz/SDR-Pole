package app.sdrpole.core.p25;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class P25TrunkingStateTest {
    @Test void resolvesPendingVoiceGrantWhenBandPlanArrives() {
        var state = new P25TrunkingState();
        var now = Instant.parse("2026-06-20T12:00:00Z");
        state.accept(new P25ControlEvent.GroupVoiceGrant(1201, 4567, 1, 100, 0, false), now);
        assertEquals(1, state.unresolvedGrantCount());

        state.accept(new P25ControlEvent.IdentifierUpdate(new P25BandPlan(1, 851_000_000, 12_500, -45_000_000)), now);
        assertEquals(0, state.unresolvedGrantCount());
        assertEquals(852_250_000, state.activeCalls().getFirst().frequencyHz());
    }

    @Test void tracksIdentityNeighborsAndExpiresCalls() {
        var state = new P25TrunkingState();
        var now = Instant.parse("2026-06-20T12:00:00Z");
        var identity = new P25SystemIdentity(0xABCDE, 0x123, 0x456);
        state.accept(new P25ControlEvent.IdentifierUpdate(new P25BandPlan(0, 769_000_000, 12_500, 30_000_000)), now);
        state.accept(new P25ControlEvent.NetworkStatus(identity, 1, 2, 0, 10), now);
        state.accept(new P25ControlEvent.AdjacentSite(1, 3, 0, 20), now);
        state.accept(new P25ControlEvent.GroupVoiceGrant(42, 99, 0, 30, 1, true), now);
        assertEquals(identity, state.identity().orElseThrow());
        assertEquals(1, state.neighbors().size());
        assertTrue(state.activeCalls().getFirst().encrypted());
        state.expireCalls(now.plusSeconds(3), Duration.ofSeconds(2));
        assertTrue(state.activeCalls().isEmpty());
    }
}
