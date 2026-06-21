# Threat model

## Protected assets

- Operator-selected locations, saved sites and scan plans
- Recordings and decoded metadata
- Radio identifiers and device access
- Decoder packages, application binaries and update provenance
- Directory credentials if authenticated sources are added later

## Trust boundaries

1. USB/network SDR hardware and native SoapySDR drivers
2. Optional external decoder processes and downloaded packages
3. Public map, FCC and future authenticated directory services
4. Local filesystem, preferences, recordings and audit records
5. GitHub/build infrastructure and release artifacts

## Priority threats and controls

| Threat | Current mitigation | Required follow-up |
|---|---|---|
| Malicious dependency/package | Curated origins, pinned SHA-256, Gradle verification, SBOM, atomic activation | Signatures and sandboxed/out-of-process decoder policy |
| Compromised CI action | Actions pinned to immutable SHAs, minimum permissions | Protected environments and artifact attestations |
| Path traversal/partial install | Normalized allowlisted filenames, staging and atomic move | Negative fuzz corpus for every archive/parser |
| Secret/location disclosure | No telemetry, support/audit redaction, no embedded credentials | OS credential vault adapter and formal privacy assessment |
| Untrusted radio/native driver crash | Adapter boundary, friendly failure path | Process isolation and resource limits |
| Unauthorized transmit/damage | Receive-only API; RF amp/bias off by default with confirmations | Independent hardware safety verification |
| Audit-log injection/growth | Control-character cleaning, structured JSONL, rotation, owner permissions | Integrity chaining and agency log export |

Encrypted radio traffic is identified but is never decrypted by SDR-Pole.
