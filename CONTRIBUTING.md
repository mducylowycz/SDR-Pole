# Contributing to SDR-Pole

SDR-Pole aims to make radio monitoring approachable without hiding whether a
feature truly works. Bug reports, device adapters, DSP tests, accessibility
improvements, and documentation are welcome.

## Before opening a pull request

1. Build with Java 21 using `cd jvm && ./gradlew build`.
2. Add tests for parsing, configuration, DSP, and device behavior.
3. Do not copy source from other SDR applications.
4. Document third-party code, binaries, licenses, and checksums.
5. Never implement decryption or market encrypted traffic as decodable.

Native libraries must remain behind a documented adapter and be built on each
supported operating system. Decoder downloads must use the verified package
installer; arbitrary executable downloads are not accepted.
