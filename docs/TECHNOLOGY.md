# Technology and dependency decisions

This document records why a component is being considered and whether it is
already implemented. A listed technology is not automatically bundled.

## Hardware abstraction

SDR-Pole uses [SoapySDR](https://github.com/pothosware/SoapySDR) as its primary
receive-only hardware API. SoapySDR is Boost-licensed and exposes separate
driver modules for hardware families. The current Java bridge streams CF32 IQ
from SoapySDR and has been tested with HackRF One.

Planned release driver bundles:

| Family | Soapy module | Notes |
|---|---|---|
| HackRF | SoapyHackRF | Current hardware-test target |
| RTL-SDR / Blog V4 | SoapyRTLSDR | Low-cost primary target |
| Airspy / HF+ | SoapyAirspy | High-dynamic-range options |
| SDRplay RSP | SoapySDRPlay3 | Requires vendor API terms |
| LimeSDR | SoapyLMS7 | Multi-channel hardware |
| PlutoSDR | SoapyPlutoSDR | Network/USB support |
| USRP | SoapyUHD | Timestamped and multi-channel devices |
| Remote receivers | SoapyRemote | LAN receiver support |

[SoapyMultiSDR](https://github.com/pothosware/SoapyMultiSDR) is being evaluated
for coherent/composite devices. Independent scanner tuners do not need coherent
clocking and can run as separate receiver sessions.

## DSP pipeline

Live analog audio now inserts a stateful complex integrate-and-decimate stage before
demodulation. This is particularly important for HackRF: capturing at the recommended
8 MS/s and selecting one sample every output period would alias wideband energy into
the audible channel. The current stage averages each decimation interval, then applies
mode-aware demodulation, audio low-pass conditioning, and bounded level control.

This is an initial anti-alias implementation, not the final multi-channel engine. The
next receiver architecture uses a designed FIR/polyphase channelizer so multiple
channels inside one tuner passband share filtering work and retain quantified stop-band
performance. See the [GNU Radio channelizer model](https://wiki.gnuradio.org/index.php/Polyphase_Channelizer).

Implemented now:

- CF32 streaming through the SoapySDR C ABI via JNA
- configurable frequency, sample rate, gain, and automatic-gain request
- Hann-window FFT and scrolling waterfall
- center-channel phase-discriminator NFM and 48 kHz Java Sound output
- hardware-gated test that requires real IQ and spectrum samples

Before release, the simple NFM path will gain a proper channel filter,
fractional resampler, adaptive squelch, DC/IQ correction, and calibration. A
polyphase FFT channelizer is planned when several channels share one wideband
tuner; a heterodyne channelizer remains useful for a small channel count.

## P25 and voice codecs

P25 support is two distinct layers:

1. SDR-Pole must demodulate C4FM/CQPSK, recover symbols, decode P25 frames and
   control-channel messages, and allocate traffic channels.
2. IMBE/AMBE codec frames require a compatible voice library.

[JMBE](https://github.com/DSheirer/jmbe) 1.0.9 exposes IMBE and AMBE audio
conversion to Java. It is GPL-3.0 and carries an upstream patent warning.
SDR-Pole therefore does not silently bundle or activate it. The Decoder Library
opens the official creator download and validates a JAR selected by the user.

[OP25](https://github.com/boatbod/op25) is a useful standards and interoperability
reference and supports P25 trunking Phase 1/2. It is not copied into this
clean-room codebase. Any future OP25 process adapter must preserve GPL terms and
remain clearly identified as a separately installed engine.

The clean-room trunking state layer models validated WACN/System/NAC identities,
channel-identifier band plans, current and adjacent sites, group voice grants,
Phase 2 slots, encryption indicators, pending grants, and call expiry. It starts
with decoded control events; C4FM/LSM symbol recovery, framing, CRC/FEC, and
TSBK/MBT parsing remain separate required stages.

## Fast scanning

Ordinary SDRs scan with overlapping FFT windows covering 72% of the selected
sample rate. This replaces thousands of per-channel retunes with a few wideband
measurements while avoiding unreliable passband edges. Turbo, Fast, Balanced,
and Deep dwell modes trade speed for weak-signal confidence.

HackRF uses the official firmware-assisted `hackrf_sweep` path for arbitrary
custom ranges and full 1–6000 MHz discovery. SDR-Pole always invokes sweep mode
with the RF amplifier and antenna-port power disabled, ranks bins relative to the
measured noise floor, merges adjacent peaks, and labels results `MEASURED` rather
than claiming an identity. Decoder/frame synchronization remains the second
stage for protocol confirmation.

Encrypted calls may be detected, labeled, logged, or muted. SDR-Pole will not
attempt to defeat encryption.

## RF front-end protection

Hardware protection is profile-driven and happens before native driver calls.
For HackRF One, SDR-Pole uses the vendor's recommended safe start: RF amplifier
off, LNA 16 dB, and VGA 16 dB. LNA is clamped to 0–40 dB in 8 dB steps and VGA
to 0–62 dB in 2 dB steps. Antenna-port power and the near-antenna RF amplifier
are driven off on every open unless the user accepts a targeted warning. The UI
states the HackRF maximum input of -5 dBm.

Software cannot sense or block excessive external RF at the SMA port. Users near
transmitters still need a suitable external attenuator and band filter. Antenna
power is limited by hardware to approximately 3.0–3.3 V and 50 mA and must only
be used with compatible active accessories.

Sources: [HackRF gain controls](https://hackrf.readthedocs.io/en/latest/setting_gain.html),
[HackRF One limits](https://hackrf.readthedocs.io/en/latest/hackrf_one.html), and
[SoapyHackRF settings](https://github.com/pothosware/SoapyHackRF/blob/master/HackRF_Settings.cpp).

## Site maps

The map requests only tiles visible in a human-controlled viewport, identifies
SDR-Pole with a stable User-Agent, retains tiles locally for at least seven days,
does not prefetch, and displays OpenStreetMap attribution. This follows the
current [OSM tile usage policy](https://operations.osmfoundation.org/policies/tiles/).

## User-experience requirements

- Start Listening always runs hardware, signal, decoder, storage, and audio
  preflight checks.
- Basic mode chooses safe tuner defaults; Advanced exposes the real values.
- Errors name the repair action and preserve a technical support detail.
- Decoder packages display source, version, license, platform, checksum, and
  status before activation.
- New native/decoder packages are staged, verified, atomically activated, and
  reversible.
