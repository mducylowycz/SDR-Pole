# SDR-Pole

SDR-Pole is an open-source, local-first radio monitoring application designed
to make SDR scanning dramatically easier on Windows, macOS, and Linux.

The product goal is simple: connect one or more SDRs, choose an area or system,
and press **Start listening**. Automatic preflight checks should handle device
drivers, tuner assignment, gain, sample rates, decoder packages, and audio
without exposing a wall of cryptic errors.

> [!IMPORTANT]
> SDR-Pole is early-stage software. Device discovery, the cross-platform UI,
> plugin API, verified downloader, catalog, tests, and packaging are working.
> Live IQ reception and P25 audio decoding are active roadmap work—not finished
> features. Encrypted traffic is never decrypted.

## Current features

- Java 21 + JavaFX desktop application with a bundled runtime
- Vendor-neutral SoapySDR discovery and live CF32 IQ streaming with HackRF fallback
- Multiple-device data model for parallel receivers
- HackRF One detection verified on macOS
- Hardware-tested live HackRF stream, FFT waterfall, tuner controls, and NFM audio
- Decoder plugin SPI using Java `ServiceLoader`
- Staged decoder downloads with SHA-256 verification and atomic activation
- Visible P25 Phase 1/2, DMR, and NXDN package/license status
- Official JMBE Creator link plus local JMBE JAR validation
- Guided home, device, system, call, spectrum, decoder, recording, map,
  diagnostics, and settings areas
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

See [architecture](docs/ARCHITECTURE.md), [technology decisions](docs/TECHNOLOGY.md),
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
