# HackRF platform integration

SDR-Pole treats HackRF as a hardware family, not as a generic tuner. Discovery is
read-only, receive-only, serial-aware, and version-aware. It never flashes firmware,
enables transmit, changes clock output, or powers the antenna port during discovery.

## Hardware model

The receive path combines an RFFC5072 mixer/synthesizer, a MAX2837 transceiver
(MAX2839 on HackRF One r9), a MAX5864 ADC/DAC, an LPC43xx microcontroller, and a
CoolRunner-II CPLD. HackRF Pro replaces the CPLD with an iCE40UP5K FPGA and uses a
MAX2831 transceiver. The Si5351 clock generator, external clock connectors, RF
switches, USB interface, flash, expansion headers, and antenna-port power are also
material capabilities—not incidental details.

This explains several product decisions:

- HackRF One is a half-duplex, 8-bit IQ radio covering 1 MHz–6 GHz. SDR-Pole exposes
  reception only.
- HackRF Pro covers 100 kHz–6 GHz and adds a TCXO, FPGA processing, shielding,
  improved protection, hardware TX disable, DC-spike removal, and optional precision
  modes.
- The RF amplifier, 0–40 dB LNA/IF gain, 0–62 dB VGA/baseband gain, and antenna-port
  power are separate controls. They must never be collapsed into a fictitious single
  “gain” control for HackRF.
- HackRF One's maximum safe input is −5 dBm. Software cannot guarantee protection
  from a strong external signal, so SDR-Pole defaults the RF amplifier and antenna
  power off and recommends external attenuation/filtering near transmitters.

Sources: [official hardware components](https://hackrf.readthedocs.io/en/stable/hardware_components.html),
[HackRF One](https://hackrf.readthedocs.io/en/stable/hackrf_one.html), and
[HackRF Pro](https://hackrf.readthedocs.io/en/latest/hackrf_pro.html).

## Sampling and DSP policy

Although the host API accepts rates down to 2 MS/s, the MAX5864 is not specified
below 8 MHz and the MAX2837's narrowest baseband filter is 1.75 MHz. SDR-Pole
therefore offers 8, 10, 16, and 20 MS/s as native HackRF receive rates, starts at
8 MS/s, then channelizes and decimates narrow signals in software.

HackRF Pro's optional gateware creates two future operating profiles:

- 4-bit half precision at up to 40 MS/s for wide spectrum monitoring where bandwidth
  matters more than dynamic range.
- 16-bit extended precision with 16x/32x/64x/128x decimation and typical 9–11 ENOB
  for weak or narrowband signals.

These modes will not appear as ordinary sample-rate choices until the native stream
format, gateware state, USB capacity, and decoder compatibility can all be verified.

Sources: [sampling and baseband filters](https://hackrf.readthedocs.io/en/latest/sampling_rate.html)
and [HackRF Pro gateware](https://hackrf.readthedocs.io/en/latest/gateware.html).

## Current integration

- `hackrf_info` inventory: board family, serial, part ID, firmware, firmware API,
  host-tool and library versions.
- Firmware/host mismatch warning because firmware and host tools should be maintained
  as a compatible set.
- Dedicated device card showing usable range, precision, native-rate policy, sweep,
  clock-tool, and Opera Cake readiness.
- Safe gain defaults: RF amp off, LNA 16 dB, VGA 16 dB, antenna power off.
- Native `hackrf_sweep` discovery from custom ranges through the entire supported
  HackRF One range, with results ranked against the measured noise floor.
- Device-aware 8–20 MS/s receive choices with software decimation.
- Serial-preserving discovery so multiple HackRF devices can be assigned
  deterministically.

## Next hardware-aware increments

1. Read external 10 MHz clock detection and display readiness without changing clock
   output. Multi-radio coherent operation also requires shared hardware triggering;
   a common clock alone is not synchronization.
2. Add an Opera Cake wizard. Frequency mode can select antennas or filters by tuned
   band; time mode can support pseudo-Doppler experiments. Persistent switch plans
   must be previewed and explicitly approved.
3. Add measured USB-throughput health and dropped-sample telemetry. High rates need a
   reliable High-Speed USB path and sufficient 5 V / 500 mA power.
4. Add calibration profiles for oscillator error, gain/noise-floor history, DC offset,
   and per-band antenna/filter selection.
5. Negotiate HackRF Pro gateware capabilities before enabling 4-bit or 16-bit streams.
6. Add offset-tuning assistance for HackRF One's center-frequency DC spike while
   keeping Pro's hardware distinction accurate.

Sources: [synchronization checklist](https://hackrf.readthedocs.io/en/latest/synchronization_checklist.html),
[minimum host requirements](https://hackrf.readthedocs.io/en/latest/hackrf_minimum_requirements.html),
[Opera Cake modes](https://hackrf.readthedocs.io/en/latest/opera_cake_modes_of_operation.html),
and [troubleshooting](https://hackrf.readthedocs.io/en/latest/troubleshooting.html).
