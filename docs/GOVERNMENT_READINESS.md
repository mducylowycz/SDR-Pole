# Government readiness profile

Updated: 2026-06-20

SDR-Pole uses NIST SP 800-218 SSDF 1.1 as its secure-development vocabulary.
This is an evidence roadmap for acquisition and assessment; it is not a claim
of agency approval, certification, authorization, or operational suitability.

| Area | Implemented evidence | Remaining assessment/work |
|---|---|---|
| Secure build | Java 21 toolchain, Gradle wrapper, dependency SHA-256 verification, immutable CI action pins | Reproducible byte-for-byte release study; protected release environment |
| Component transparency | CycloneDX 1.6 JSON/XML SBOM generated in CI and retained as an artifact | Release-linked signed SBOM attestation |
| Vulnerability discovery | Tests on three operating systems, scheduled/push/PR CodeQL extended analysis, Dependabot | Defined severity/remediation SLAs and external penetration test |
| Release integrity | Pinned decoder checksums and atomic package activation | Platform code-signing identities, notarization, signed update channel and provenance attestation |
| Privacy | Local-first storage, no telemetry, redacted support report and audit fields | Formal privacy threshold analysis for each deployment |
| Accountability | Local rotating JSONL operational audit log | Agency-selected retention, export, time synchronization, centralized log integration |
| Accessibility | Keyboard map, waterfall, tuning wheel and navigation; accessible names/help | Manual assistive-technology testing and a completed VPAT/ACR |
| Deployment | Bundled runtime and offline frequency guide | Managed/air-gapped installation guide, configuration policy and enterprise package formats |
| Radio security | Receive-only architecture and protected RF controls | Independent safety review and deployment-specific spectrum/legal authorization |

References:

- [NIST SP 800-218 SSDF 1.1](https://csrc.nist.gov/pubs/sp/800/218/final)
- [CISA SBOM resources](https://www.cisa.gov/topics/cyber-threats-and-advisories/sbom/sbomresourceslibrary)
- [Section 508 applicability and conformance](https://www.section508.gov/develop/applicability-conformance/)
