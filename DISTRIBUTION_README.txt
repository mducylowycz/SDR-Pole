SDR-POLE EARLY ACCESS
=====================

Start the app
-------------
Double-click SDR-Pole.app. If macOS asks for confirmation, Control-click the
app, choose Open, then choose Open again.

Connect a HackRF
----------------
1. Use a USB data cable and connect the HackRF directly to the Mac when possible.
2. Close SDRTrunk, GopherTrunk, and other SDR programs. Only one program can own
   a HackRF receive stream at a time.
3. Open Devices and choose Refresh devices.
4. Open Live Tuner, select the HackRF, enter a known analog NFM frequency, and
   choose Listen.

If no radio appears
-------------------
Open Diagnostics in SDR-Pole. “macOS does not currently see an SDR on USB”
means this is below the application layer: reconnect or power-cycle the radio,
try another data-capable cable or USB port, and avoid an unpowered hub.

P25 and JMBE
------------
The Decoder Library links to the official JMBE Creator and validates a JMBE JAR
you select. P25 frame and trunk-control decoding is still under development;
JMBE alone cannot decode a radio signal.

Project and support information
-------------------------------
https://github.com/mducylowycz/SDR-Pole
