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

## Receiver engine

- [x] SoapySDR native streaming bridge
- [ ] Multiple simultaneous IQ streams
- [x] FFT spectrum and waterfall
- [ ] Channelizer, resampler, squelch, and automatic gain
- [x] Initial live center-channel NFM audio output
- [x] Initial center-channel NFM, WFM, AM, USB, LSB, and CW demodulation
- [ ] Production channel filters, resampler, squelch, stereo WFM, and selectable audio output
- [ ] Recording and replay fixtures for deterministic tests

## Digital voice and trunking

- [ ] P25 Phase 1 C4FM/CQPSK symbol and frame decoding
- [ ] P25 control-channel messages and system/site discovery
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
- [ ] Automatic tuner allocation by bandwidth and priority
- [ ] Live calls, hold, lockout, priorities, aliases, and alerts
- [ ] Searchable call history and configurable recording retention
- [ ] Sites, neighbor lists, and optional mapping
- [ ] One-click support report with sensitive-data redaction
- [ ] Accessible keyboard navigation, screen-reader labels, and localization
