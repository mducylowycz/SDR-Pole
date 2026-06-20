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

Encrypted calls may be detected, labeled, logged, or muted. SDR-Pole will not
attempt to defeat encryption.

## User-experience requirements

- Start Listening always runs hardware, signal, decoder, storage, and audio
  preflight checks.
- Basic mode chooses safe tuner defaults; Advanced exposes the real values.
- Errors name the repair action and preserve a technical support detail.
- Decoder packages display source, version, license, platform, checksum, and
  status before activation.
- New native/decoder packages are staged, verified, atomically activated, and
  reversible.
