# SDR-Pole product specification

## Product promise

SDR-Pole is one approachable, cross-platform application for trunked-radio listening and general SDR exploration. A beginner should reach first audio without understanding gain stages, channelizers, playlists, or Java libraries; an expert should still be able to inspect and control them.

## Usability contract

- The Home screen begins with the user's goal, not the application's object model.
- Simple mode shows frequency, mode, radio, a few recognizable examples, and one Listen button. Lab mode reveals sample rate, gain, and future DSP routing.
- Every readiness label is computed from current state. Planned features are never painted as operational.
- Hardware selection and safe defaults flow forward; users do not repeatedly select the same radio.
- Error messages state what happened and the next repair action without exposing native error walls.
- Location and radio serial numbers stay out of copied diagnostics by default.
- Settings that people reasonably expect to persist—mode, last frequency, complexity level, and manually entered location—persist locally.

## Primary experiences

### Scanner

1. Detect radios and explain conflicts or missing drivers.
2. Find or import a system near a chosen location.
3. Run a preflight covering bandwidth, tuners, audio, protocol decoder, and vocoder.
4. Press Listen; the scheduler assigns control and traffic channels across available tuners.
5. Show calls, talkgroups, radios, encryption state, recording state, and plain-language failures.

### Explore Spectrum

1. Select a radio, frequency, mode, bandwidth, sample rate, gain, and antenna.
2. View FFT/waterfall, tune by click or direct entry, and hear AM/NFM/WFM/SSB/CW.
3. Save frequency presets, scan ranges, mark signals, and record audio or SigMF IQ.
4. Attach observations (time, location, signal level) without claiming they locate a transmitter.

## Architecture

- **Device layer:** SoapySDR adapters, capabilities, hot-plug discovery, exclusive-use diagnostics.
- **IQ engine:** bounded buffers, timestamped blocks, resampling, DC/IQ correction, spectrum generation.
- **Channel engine:** polyphase channelizer and per-channel demodulator chains.
- **Protocol engine:** isolated decoder plugins with test vectors and capability declarations.
- **Voice engine:** audio routing plus optional, separately licensed vocoders such as JMBE.
- **Directory engine:** FCC ULS, user imports, optional authenticated providers, cache provenance and age.
- **Storage:** systems, aliases, calls, observations, recordings, migrations, portable backup.
- **UI:** Simple mode uses task wizards; Lab mode exposes the signal graph and detailed controls.

## Definition of “one-click decoder install”

The app shows terms, downloads only from an approved upstream, verifies integrity, installs outside the application bundle, validates compatibility, activates atomically, and offers repair/remove. Unsupported platforms remain clearly unavailable. No decoder is silently downloaded or executed.

## Release gates

- Hardware and decoder capability labels are factual.
- Each supported protocol has licensed test vectors and regression tests.
- Audio underruns, USB loss, device contention, and corrupt packages produce repairable messages.
- Windows, macOS, and Linux packages pass clean-machine smoke tests.
- Location data always displays source and freshness; private coordinates remain local by default.
