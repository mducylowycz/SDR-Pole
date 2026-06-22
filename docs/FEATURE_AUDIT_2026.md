# Full feature audit — June 2026

This audit separates implemented behavior, verified behavior, and roadmap intent.
A visible button is not considered a feature unless its underlying path is operational
and reports failure in plain language.

## Product benchmark

Mature software proves the technical ceiling but also exposes the usability problem:

- SDRTrunk supports multiple tuners, P25 Phase 1 trunk following, playlists, aliases,
  recording, streaming, preferred tuners, and decoder-assisted PPM correction. Its
  playlist/channel/alias model is powerful but requires users to learn the data model
  before listening. Sources: [SDRTrunk manual](https://github.com/DSheirer/sdrtrunk/wiki/User-Manual),
  [tuners](https://github.com/DSheirer/sdrtrunk/wiki/Tuners), and
  [playlist editor](https://github.com/DSheirer/sdrtrunk/wiki/Playlist-Editor).
- SDRangel demonstrates the breadth expected of a general SDR workstation: multiple
  simultaneous demodulators and radios, remote devices, signal measurements, scanning,
  mapping, heat maps, hardware control, and a plugin/API architecture. That breadth is
  valuable; exposing it as device sets plus channels plus plugins is not an acceptable
  beginner workflow for SDR-Pole. Source: [SDRangel feature catalog](https://www.sdrangel.org/).
- GNU Radio's polyphase channelizer partitions one wide sample stream into efficiently
  decimated channels. That is the correct long-term architecture for many conventional
  or trunked channels inside one tuner passband. Source: [GNU Radio polyphase
  channelizer](https://wiki.gnuradio.org/index.php/Polyphase_Channelizer).

## Feature-by-feature assessment

| Area | Operational now | Material gap | Product direction |
|---|---|---|---|
| First run | Goal-first Home, live radio/audio/library checks | Antenna education and one-click repair installation | A three-step connect/location/listen walkthrough with no advanced vocabulary |
| Hardware | Soapy discovery, serial identity, HackRF fallback and protection | Full capability queries, hot-plug events, calibrated PPM, USB drop telemetry | One capability profile per physical device; never show unsupported controls |
| HackRF | One/Pro inventory, firmware/API checks, native sweep, gain/power interlocks | Clock readout, Opera Cake wizard, Pro precision modes | Hardware-aware profiles negotiated at runtime |
| Spectrum | FFT/waterfall, cursor, click tuning, virtual wheel | Averaging/peak hold, calibrated dBFS, bandwidth overlay, measurement tools | Simple tuner by default; measurement workbench on demand |
| Analog audio | AM/NFM/WFM/SSB/CW, selected audio route, test tone, anti-alias decimation, conditioning | Production FIR/polyphase filters, stereo WFM, user squelch, recording | Mode-aware channel filters and explicit signal/audio health |
| Scanning | Named and custom ranges, wide FFT windows, HackRF sweep, stable peak selection | Lockouts, priority/watch lists, resumable survey, signal history UI | Scan ranges once per RF window and channelize candidates concurrently |
| Frequency data | SQLite, provenance/confidence, NOAA seed, local observations, provider catalog | Automated bulk regulatory imports and delta updates | Location creates a ready-to-listen offline index with visible source age |
| Maps | Pan/zoom, location selection, site pins, tile cache | Result clustering, coverage layers, decoded neighbors, privacy controls | Map is a location and system picker—not decoration |
| P25 | Site storage/import, directory sync, runtime configuration/process ownership, allocation model, state engine | Verified symbol/frame path, runtime telemetry, grant-driven tuning and vocoder audio | One action selects/validates control channels and follows legal clear calls |
| Other digital modes | Package catalog/plugin boundary | Operational DMR/NXDN/AIS/ADS-B/etc. decoders | Install only signed/verified packages with explicit license and test vectors |
| Calls | Staged listening-health model | Live call queue, hold/lockout/priority, history, recordings | Scanner-like controls using names and outcomes, not decoder internals |
| Multi-radio | Device model and P25 assignment plan | Concurrent IQ streams, dynamic allocation, coherent timing | Automatic roles with manual override only in Lab mode |
| Accessibility | Keyboard foundations and accessible help | Screen-reader/manual Section 508 verification and localization | Accessibility is a release gate, not a settings option |
| Security | Receive-only policy, checksums, SBOM, CodeQL, protected settings, redacted audit | Signed releases, notarization, provenance attestations, independent assessment | Consumer-simple distribution with government-ready evidence underneath |

## Refinements implemented from this audit

1. Added selectable audio outputs and a short test tone directly in Setup.
2. Persisted the selected speaker and routed live audio through it.
3. Added a stateful complex integration/decimation stage before analog demodulation,
   reducing wideband alias energy when HackRF runs at its correct 8 MS/s native rate.
4. Added mode-aware audio low-pass conditioning and bounded automatic level control.
5. Split audio routing, DSP, and Setup presentation into focused classes rather than
   growing the application shell.
6. Corrected P25 status reporting so installed, running, synchronized, granted, decoded,
   and audible are never collapsed into one misleading state.

## Next execution order

1. Measured Soapy capability profiles and dropped-sample telemetry.
2. Proper FIR/polyphase channelizer with channel bandwidth and squelch controls.
3. End-to-end P25 runtime telemetry adapter and deterministic IQ test fixtures.
4. Concurrent channel/tuner scheduler for grants and conventional watch lists.
5. Resumable FCC/ISED/ACMA/Ofcom ingestion with source-age UI.
6. Live calls, hold/lockout/priority, recording, and searchable history.
7. Signed update pipeline, platform notarization/signing, and accessibility audit.
