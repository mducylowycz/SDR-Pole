# SDR-Pole product research

Updated: 2026-06-20

This document records workflow and architecture research. SDR-Pole is a clean-room implementation: we study public behavior, documentation, interfaces, and failure modes; we do not copy source code. Dependencies must be reviewed independently for license compatibility.

## What mature tools teach us

### SDRTrunk

SDRTrunk demonstrates pooled multi-tuner channel allocation, P25 Phase 1/2 trunk following, playlist-backed systems, baseband recording/playback, spectrum displays, automatic PPM correction, and selectable channelizers. Its power is real, but device setup, playlists, aliases, channels, and decoder prerequisites are spread across separate concepts.

SDR-Pole improvement: a guided system wizard produces those underlying objects, a preflight explains missing pieces, and an Expert view exposes the details without making them the entry point.

Sources: [project wiki](https://github.com/DSheirer/sdrtrunk/wiki), [tuners](https://github.com/DSheirer/sdrtrunk/wiki/Tuners), [user manual](https://github.com/DSheirer/sdrtrunk/wiki/User-Manual), [playlist editor](https://github.com/DSheirer/sdrtrunk/wiki/Playlist-Editor).

### SDRangel

SDRangel demonstrates a broad Rx/Tx device-channel-feature architecture, a REST-controlled headless server, mapping, satellite tracking, and Doppler support. The same flexibility creates a steep object-model and workspace learning curve.

SDR-Pole improvement: keep the composable engine, but offer two task-oriented experiences—**Scanner** and **Explore Spectrum**—with reusable presets and a visible signal path. Advanced routing and remote control belong in Lab mode.

Sources: [SDRangel repository](https://github.com/f4exb/sdrangel), [server documentation](https://github.com/f4exb/sdrangel/wiki/SDRangel-server), [feature plugins](https://github.com/f4exb/sdrangel/wiki/Feature-plugins).

## Technology decisions

- **Hardware abstraction:** SoapySDR provides a vendor-neutral API, multi-device enumeration, streaming, and SoapyRemote. [SoapySDR](https://github.com/pothosware/SoapySDR)
- **Wideband channelization:** use polyphase filter banks when one device must serve multiple simultaneous channels efficiently. [GNU Radio PFB notes](https://wiki.gnuradio.org/index.php/Polyphase_Filterbanks)
- **Recording interchange:** write SigMF metadata/data pairs, including capture frequency and optional geolocation, so recordings are portable. [SigMF](https://sigmf.org/)
- **Local records:** FCC ULS open data is public-domain evidence about licenses and locations, not proof that a transmitter is active. [FCC ULS open data](https://opendata.fcc.gov/Wireless/FCC-Universal-Licensing-System-ULS-/x28i-i4z4)
- **Curated directory:** RadioReference access must honor its current API terms, developer-key approval, and per-user Premium-account requirements. SDR-Pole must never ship shared credentials or clone the website. [RadioReference API policy](https://support.radioreference.com/hc/en-us/articles/18844460198932-Database-Web-Service-API)
- **ISM decoding:** rtl_433 is a candidate external decoder host for supported sensors and already works with SoapySDR; integration requires process isolation and license review. [rtl_433](https://github.com/merbanan/rtl_433)
- **Direction finding:** coherent receivers such as KrakenSDR support phase-based bearing estimation and multi-site triangulation. A single HackRF does not. [KrakenSDR overview](https://www.krakenrf.com/about-krakensdr)

## Decoder safety and usability

Every downloadable package needs a versioned manifest, HTTPS origin, pinned SHA-256 or signature, license/patent notice, staged install, validation, activation, and rollback. JMBE is the first complete implementation of this path. It is a vocoder library, not a P25 signal/frame decoder.

No UI may label a decoder “Installed” merely because a catalog entry exists. Operational status means the full signal path has passed a test vector: tuner → channelizer → demodulator → protocol framing → vocoder (if needed) → audio/metadata.
