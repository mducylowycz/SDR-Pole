# SDR-Pole architecture

## Product promise

A new user should go from unopened installer to understandable radio audio in
five minutes: install, connect SDR, choose an area/system, install any required
decoder package with its license shown, and press **Start listening**.

## Platform

- Java 21 LTS for the shared engine and application model.
- JavaFX 21 LTS for Windows, macOS, and Linux desktop UI.
- Gradle toolchains for reproducible builds.
- `jlink`/`jpackage` installers bundling Java; users never install a JRE.
- Platform CI runners produce `.dmg`/`.pkg`, `.msi`, `.deb`, and portable ZIPs.

## Modules

- `sdr-pole-plugin-api`: stable decoder SPI and data contracts.
- `sdr-pole-core`: SDR/audio pipeline, SQLite storage, imports, plugin catalog,
  verification, rollback, and diagnostics.
- `sdr-pole-desktop`: onboarding, scanner, spectrum, calls, recordings, and
  decoder manager UI.

## Decoder packages

Packages are not arbitrary URLs. A catalog entry contains ID, semantic version,
app compatibility, OS/architecture compatibility, license, download URL,
SHA-256, and a signature. Installation is staged, verified, atomically activated,
and reversible. The user must accept any separate codec/license terms.

Java `ServiceLoader` discovers implementations of `DecoderPlugin`. Native
libraries are shipped per OS/architecture inside a decoder or hardware package.
Future hardening should run untrusted third-party decoders out-of-process.

## Ease-of-use rules

1. Never ask users for sample rate, gain, drivers, or control channels before
   automatic detection has failed.
2. Translate errors into actions: “HackRF is open in another app” rather than
   a USB exception code.
3. Advanced controls exist, but live behind an Advanced disclosure.
4. Every Start button runs a preflight: hardware, signal, decoder, audio test.
5. Configuration is portable and exportable, with automatic backups.

## Licensing

SDR-Pole is clean-room. Ideas and interoperable file formats may be studied,
but source is not copied from SDRTrunk or GopherTrunk. Dependencies and decoder
packages retain their own licenses and notices.
