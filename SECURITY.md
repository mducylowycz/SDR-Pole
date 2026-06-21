# Security policy

## Reporting a vulnerability

Do not open a public issue for a suspected vulnerability. Use GitHub's private
security-advisory reporting for this repository. Include the affected version,
platform, reproduction steps, impact, and any suggested mitigation. Do not send
credentials, recordings, precise monitoring locations, or radio serial numbers.

The project owner will acknowledge a complete report within five business days,
triage severity, coordinate a fix and disclosure date, and publish remediation
or compensating controls. No guaranteed response SLA or government support
contract exists yet.

## Security baseline

- Java dependencies and build plugins are SHA-256 verified by Gradle metadata.
- CI actions are pinned to immutable commit SHAs.
- Every build runs tests on Windows, macOS, and Linux and generates a CycloneDX SBOM.
- CodeQL performs extended security analysis on pushes, pull requests, and weekly.
- Decoder packages require HTTPS, a fixed version and checksum, staging, validation,
  atomic activation, and license disclosure.
- Operational audit events are local JSON Lines with sensitive-field redaction,
  owner-only permissions where supported, and bounded rotation.
- Telemetry is absent. Map/network and package downloads are user-visible features.

This baseline does **not** claim FIPS 140-3 validation, FedRAMP authorization,
FISMA compliance, Section 508 conformance, or an Authority to Operate (ATO).
Those require system-specific controls, assessment, evidence, and an authorizing
official—not repository documentation alone.
