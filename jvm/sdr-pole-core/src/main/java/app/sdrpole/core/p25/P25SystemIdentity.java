package app.sdrpole.core.p25;

public record P25SystemIdentity(int wacn, int systemId, int nac) {
    public P25SystemIdentity {
        if (wacn < 0 || wacn > 0xFFFFF) throw new IllegalArgumentException("WACN must be a 20-bit value");
        if (systemId < 0 || systemId > 0xFFF) throw new IllegalArgumentException("System ID must be a 12-bit value");
        if (nac < 0 || nac > 0xFFF) throw new IllegalArgumentException("NAC must be a 12-bit value");
    }

    public String display() { return "%05X-%03X / NAC %03X".formatted(wacn, systemId, nac); }
}
