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
premium subscription. SDR-Pole will not embed credentials, scrape pages, or
describe this source as a free bundled download.

Source: [RadioReference API documentation](https://wiki.radioreference.com/index.php/API).

## Map and measured signals

OpenStreetMap tiles provide geographic context. Saved/imported site coordinates
are pins. A single ordinary SDR can measure energy and strength, but cannot
derive transmitter coordinates; direction finding requires bearings from
multiple locations or coherent multi-channel hardware.
