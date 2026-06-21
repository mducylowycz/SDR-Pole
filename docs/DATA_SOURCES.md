# Frequency data sources

SDR-Pole separates a **frequency guide** from a **local directory** and a
**live RF observation**. They answer different questions and must never be
presented as interchangeable truth.

## Installed offline guide

The application bundles named North-American ranges for broadcast, aviation,
marine, weather, amateur, personal radio, public safety, and ISM use. Every
entry has a range, tuning step, suggested mode, common use, and region. This
removes blank-frequency guessing, but it does not claim a transmitter is active.

## FCC ULS

The FCC publishes daily and weekly ULS Public Access files. They contain
license and technical records. A future spatial importer will download,
checksum, normalize, join locations to frequencies, and retain the source date.
A license is evidence of an assignment, not proof that a signal is on the air.

Source: [FCC ULS public access downloads](https://www.fcc.gov/wireless/data/public-access-files-database-downloads).

## RadioReference

RadioReference's SOAP service supplies conventional and trunked-system data.
The developer needs an application key and each end user needs an active
premium subscription. SDR-Pole uses the official v18 service to resolve county
and statewide P25 systems, site coordinates, primary/alternate control channels,
modulation, TDMA-control metadata, and talkgroup names, descriptions, and
encryption status. U.S. coordinates are matched to a county through the official
Census Geocoder. Results are cached locally, ranked by distance, and refreshed
after 24 hours or a move of 25 km. Fully encrypted talkgroups are locked out in
the generated runtime profile; SDR-Pole does not attempt to decrypt them. It also
does not scrape pages or substitute guessed frequencies when credentials are
unavailable.

Source: [RadioReference API documentation](https://wiki.radioreference.com/index.php/API).
Location source: [U.S. Census Geocoder](https://geocoding.geo.census.gov/geocoder/).

## Map and measured signals

OpenStreetMap tiles provide geographic context. Saved/imported site coordinates
are pins. A single ordinary SDR can measure energy and strength, but cannot
derive transmitter coordinates; direction finding requires bearings from
multiple locations or coherent multi-channel hardware.
