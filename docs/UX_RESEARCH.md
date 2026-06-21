# SDR-Pole clean-room usability research

Updated: 2026-06-20

This is workflow research, not source-code reverse engineering. SDR-Pole studies
public documentation, issue reports, terminology, and observable failure modes;
it does not copy implementation code or private behavior from other projects.

## Repeated friction patterns

### Users must learn the application's object model first

SDRTrunk setup spans tuners, playlists, channels, decoders, aliases, audio,
recording, and streaming. Its getting-started guide explicitly directs users to
read the playlist and tuner manuals before decoding. SDRangel exposes devices,
channels, features, spectra, and workspaces as separate composable objects.

SDR-Pole response: begin with **Explore a frequency** or **Listen to a trunked
system**. A saved system owns its sites, control channels, map points, and future
talkgroups. Lab concepts remain available, but are not prerequisites.

Sources: [SDRTrunk getting started](https://github.com/DSheirer/sdrtrunk/wiki/Getting-Started),
[SDRTrunk user manual](https://github.com/DSheirer/sdrtrunk/wiki/User-Manual),
[SDRangel quick start](https://github.com/f4exb/sdrangel/wiki/Quick-start).

### Hardware success is separated from listening success

Device setup may depend on Zadig, udev rules, vendor APIs, kernel-driver
conflicts, manual PPM calibration, gain calibration, and external command-line
tests. Gqrx opens with a device configuration dialog and points users to external
hardware tools when discovery fails. SDRTrunk's calibration guide requires a
known signal and several manual spectrum steps before digital decoding.

SDR-Pole response: discover hardware automatically, apply a vendor safety
profile, carry the selected radio forward, and show the complete listening path
from radio through control channel, grant, vocoder, and speaker. Each failed step
links to one repair location.

Sources: [SDRTrunk tuner setup and calibration](https://github.com/DSheirer/sdrtrunk/wiki/Tuners),
[Gqrx usage and device setup](https://github.com/gqrx-sdr/gqrx).

### Activity without audio is ambiguous

A user can see P25 calls/events yet hear no audio, leaving several possible
causes: muted aliases, encrypted traffic, missing vocoder, audio routing, decoder
regression, or signal quality. A public SDRTrunk issue documents this exact
"events but no audio" experience.

SDR-Pole response: Listening Health never collapses all of those stages into a
single green/red indicator. Sync, CRC, control grant, encryption, voice library,
and speaker state are separate. Metadata without playable audio is not reported
as listening success.

Source: [SDRTrunk issue #1915](https://github.com/DSheirer/sdrtrunk/issues/1915).

### Important settings are powerful but poorly contextualized

Trunking tools expose modulation, preferred tuners, traffic-channel limits,
recording formats, alias actions, and protocol identifiers. These are useful,
but presenting them together turns first setup into a configuration exam.

SDR-Pole response: the P25 wizard asks for a recognizable system name, site
name, and one control frequency. Simulcast is phrased as a plain question;
traffic limits default safely. WACN/System/NAC and band plans are learned from
decoded messages when the control decoder is complete.

Source: [SDRTrunk playlist editor](https://github.com/DSheirer/sdrtrunk/wiki/Playlist-Editor).

## Interaction rules derived from the research

1. One primary action per empty state.
2. Never show a disabled feature without explaining what prerequisite is absent.
3. Never use “Ready” for only part of a signal path.
4. Preserve configuration atomically and provide edit/remove actions where it is displayed.
5. Use radio terminology only after a plain-language label.
6. Put repair actions next to the failing status, not in a distant preferences dialog.
7. Keep risky RF controls off by default and require consequence-specific confirmation.
8. Let experts reveal complexity with Lab mode; do not make beginners dismiss it repeatedly.
