# SDR-Pole

SDR-Pole is an open-source, local-first radio monitoring application designed
to make SDR scanning dramatically easier on Windows, macOS, and Linux.

The product goal is simple: connect one or more SDRs, choose an area or system,
and press **Start listening**. Automatic preflight checks should handle device
drivers, tuner assignment, gain, sample rates, decoder packages, and audio
without exposing a wall of cryptic errors.

> [!IMPORTANT]
> SDR-Pole is early-stage software. Device discovery, the cross-platform UI,
> plugin API, verified downloader, catalog, tests, packaging, and initial analog
> reception are working. P25 signal/frame decoding and trunk following are active
> roadmap work—not finished features. Encrypted traffic is never decrypted.

## Current features

- Java 21 + JavaFX desktop application with a bundled runtime
- Vendor-neutral SoapySDR discovery and live CF32 IQ streaming with HackRF fallback
- Multiple-device data model for parallel receivers
- HackRF One detection verified on macOS
- Hardware-tested live HackRF stream, FFT waterfall, tuner controls, and initial NFM/WFM/AM/SSB/CW audio
- Debounced strong-signal auto-tuning with spectrum-based decoder suggestions
- Virtual tuning wheel plus calibrated spectrum/waterfall, cursor, and click-to-tune
- Decoder plugin SPI using Java `ServiceLoader`
- Staged decoder downloads with SHA-256 verification and atomic activation
- Visible P25 Phase 1/2, DMR, and NXDN package/license status
- One-click official JMBE Creator download, pinned checksum verification, local build, validation, and activation
- Validated external GopherTrunk P25 Phase 1/2 engine registration (process integration remains roadmap work)
- Manual location and local-frequency discovery foundation with explicit data provenance
- Goal-first Quick Start with live hardware/audio/decoder readiness
- Persistent Simple and Lab modes with common analog examples and remembered tuning
- Privacy-redacted support report copied directly from Diagnostics
- HackRF LNA/VGA controls with vendor-step clamping, RF-amp confirmation, and antenna-power interlock
- Interactive pan/zoom OpenStreetMap site map with visible-tile caching and attribution
- Tested P25 state engine for identities, band plans, neighboring sites, voice grants, slots, and encryption state
- Guided home, device, nearby, system, call, spectrum, decoder, recording, map,
  diagnostics, and settings areas
- Goal-based Trunking Workstation and Scanner navigation instead of implementation-oriented menus
- Click-to-select map location with saved trunking-site pins
- Bundled named frequency-range guide and true multi-range strong-signal scanning
- Cross-platform Gradle wrapper and GitHub Actions build matrix

Planned support includes HackRF, RTL-SDR, Airspy, SDRplay, LimeSDR, PlutoSDR,
USRP, and network receivers through SoapySDR drivers.

## Build

Install a Java 21 JDK, then:

```sh
cd jvm
./gradlew build
./gradlew :sdr-pole-desktop:run
```

Build a self-contained macOS installer:

```sh
./gradlew :sdr-pole-desktop:macInstaller
```

See [architecture](docs/ARCHITECTURE.md), [frequency data sources](docs/DATA_SOURCES.md), [product research](docs/PRODUCT_RESEARCH.md),
[product specification](docs/PRODUCT_SPEC.md), [usability research](docs/UX_RESEARCH.md), [technology decisions](docs/TECHNOLOGY.md),
[roadmap](docs/ROADMAP.md), and [contributing](CONTRIBUTING.md).

## Decoder and hardware policy

The app is clean-room and does not copy SDRTrunk, GopherTrunk, or OP25 source.
Optional GPL or otherwise separately licensed engines can be integrated through
documented process/package boundaries while retaining their license notices.
Decoder packages must be versioned, platform-specific, checksum-verified, and
rollback-safe. SDR-Pole does not bypass encryption.

## License

SDR-Pole's original source is available under the [MIT License](LICENSE).
Optional drivers, decoder engines, and vocoders retain their own licenses.
