# Product roadmap

Checked items are implemented in the repository, not merely mocked in the UI.

## Foundation

- [x] Java 21 and JavaFX cross-platform application
- [x] Self-contained macOS app image and DMG
- [x] Decoder plugin API and verified package installer
- [x] Multi-device model and SoapySDR discovery
- [x] Curated decoder catalog with license visibility
- [x] Windows, macOS, and Linux CI build verification
- [ ] Signed release automation and auto-updater
- [x] CycloneDX 1.6 aggregate SBOM in CI
- [x] Gradle dependency checksum verification metadata
- [x] Immutable commit pins for CI actions
- [x] CodeQL extended security scanning and Dependabot policy
- [x] Privacy-redacted rotating local audit log
- [x] Threat model and government-readiness evidence matrix
- [ ] Signed build provenance and SBOM attestations
- [ ] Independent security assessment and remediation SLA
- [ ] Platform signing/notarization identities and protected release environment

## Beginner experience

- [x] Goal-first Home screen with live readiness state
- [x] Simple/Lab progressive disclosure with persistent choice
- [x] Quick analog presets and remembered frequency/mode
- [x] Radio selection carried from Devices into the tuner
- [x] Privacy-redacted support report clipboard action
- [ ] First-run antenna and local-frequency walkthrough
- [ ] Integrated repair actions for drivers, audio devices, and occupied radios
- [x] Guided P25 site wizard with plain-language simulcast choice
- [x] Editable/removable persistent system library
- [x] Flexible P25 CSV site import with common directory-header aliases
- [x] One-click automatic P25 configuration and local engine startup
- [x] End-to-end Listening Health path with next repair action
- [x] Goal-based Trunking Workstation and Scanner top-level navigation
- [x] Shared click-to-select map location and saved trunking-site pins
- [x] Offline named North-American range guide
- [x] Multi-range scan plan with stable-signal stop
- [x] Wide-FFT Turbo/Fast/Balanced/Deep scanning across named or arbitrary ranges
- [x] Safe native HackRF 1–6000 MHz hardware sweep with noise-relative peak ranking and SQLite persistence
- [x] Zero-import P25 discovery across 700/800 MHz, VHF, and UHF candidate bands with automatic runtime configuration
- [x] Accelerated drag/scroll tuning wheel with fast, fine, double-click, and keyboard gestures
- [ ] FCC ULS bulk download, spatial join, and dated local index
- [x] Versioned SQLite directory schema with source provenance, confidence, indexed location queries, and transactional upserts
- [x] Automatic NOAA reference-channel seed and persistent local strong-signal survey records
- [x] Independent provider catalog for FCC ULS, ISED TAFL, ACMA RRL, Ofcom SIS, RadioReference, and local measurement
- [ ] First-run FCC ULS land-mobile importer with download-size consent, resumable transfer, spatial join, and daily deltas
- [ ] ISED TAFL, ACMA RRL, and Ofcom SIS import adapters with fixture/terms tests
- [x] Extract authenticated location-directory orchestration from the application shell and enforce a class-size build ratchet
- [ ] Continue decomposing the legacy application shell below 800 lines, lowering the ratchet after each focused controller/pane extraction
- [x] Authenticated RadioReference v18 adapter (requires application key and user premium account)
- [x] Census coordinate-to-county lookup, statewide/county system merge, nearest-site ranking, and 24-hour cache refresh policy
- [x] Automatic P25 talkgroup label/encryption synchronization, offline persistence, and runtime alias export

## Receiver engine

- [x] Debounced strong-signal auto-tuning within the live receiver passband
- [x] Spectrum-based decoder suggestions with explicit frame-confirmation status
- [x] Virtual tuning wheel with selectable tuning steps
- [x] Combined calibrated spectrum/waterfall with cursor and click-to-tune

- [x] SoapySDR native streaming bridge
- [ ] Multiple simultaneous IQ streams
- [x] FFT spectrum and waterfall
- [ ] Channelizer, resampler, squelch, and automatic gain
- [x] Initial live center-channel NFM audio output
- [x] Initial center-channel NFM, WFM, AM, USB, LSB, and CW demodulation
- [ ] Production channel filters, resampler, squelch, stereo WFM, and selectable audio output
- [ ] Recording and replay fixtures for deterministic tests
- [x] HackRF LNA/VGA element controls and safe vendor-step clamping
- [x] RF amplifier and antenna-port power default-off interlocks
- [x] HackRF board/serial/firmware/API inventory and host-tool compatibility assessment
- [x] HackRF-native 8–20 MS/s receive policy with narrow-channel software decimation
- [ ] Read-only external-clock readiness and dropped-sample/USB health telemetry
- [ ] Guided Opera Cake antenna/filter plans with explicit preview and approval
- [ ] Negotiated HackRF Pro half-precision and extended-precision stream profiles
- [ ] Per-device capability interrogation and safety profiles beyond HackRF

## Digital voice and trunking

- [x] Process-isolated P25 Phase 1/2 runtime bridge with generated private configuration
- [x] Simulcast-aware CQPSK/C4FM selection, AFC, encrypted-call skipping, and bounded recording defaults
- [ ] P25 Phase 1 C4FM/CQPSK symbol and frame decoding
- [ ] P25 control-channel messages and system/site discovery
- [x] P25 identity, band-plan, neighbor-site, voice-grant, slot, and call-lifetime state engine
- [ ] P25 talkgroup following across one or more tuners
- [x] One-click JMBE Creator download, checksum verification, build, validation, and activation
- [ ] Connect validated JMBE audio to decoded IMBE/AMBE frames
- [ ] P25 Phase 2 TDMA traffic channels
- [ ] AMBE+2 vocoder package with explicit terms
- [ ] DMR and NXDN packages
- [ ] Encryption detection and automatic muting (no decryption)

## Public-safety scanner experience

- [x] Location model and honest separation of license records, RF observations, and direction finding
- [ ] FCC ULS regional importer, provenance, cache age, and update workflow
- [ ] RadioReference-assisted import using the user's authorized account
- [ ] SDRTrunk playlist migration
- [x] Initial automatic tuner allocation with nearest-site, wideband control, virtual voice taps, and additional voice radios
- [ ] Dynamic tuner reallocation by live grant priority and measured RF quality
- [ ] Live calls, hold, lockout, priorities, aliases, and alerts
- [ ] Searchable call history and configurable recording retention
- [x] Interactive pan/zoom site map with control-channel markers
- [ ] Feed decoded P25 neighbor-site events into persisted map layers
- [ ] One-click support report with sensitive-data redaction
- [x] Keyboard and accessible-help foundations for navigation, map, tuning, and scanner
- [ ] Manual Section 508 assistive-technology testing and completed ACR/VPAT
- [ ] Localization
