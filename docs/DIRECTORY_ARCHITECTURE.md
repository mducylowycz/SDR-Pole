# Frequency directory architecture

SDR-Pole builds one local, queryable view from independent sources. SQLite is the
index and offline cache; it is not a new source of truth. Every channel retains a
provider ID, provider record ID, update time, location accuracy, and evidence level.

## Evidence levels

- `REFERENCE`: a published channel or allocation, such as the seven NOAA Weather
  Radio channels. It may or may not be receivable at a particular point.
- `LICENSED`: a regulator authorized a frequency near a site. This is not proof
  that the station is built, active, receivable, or a trunking control channel.
- `COMMUNITY`: a user-maintained directory describes the channel. Provider terms
  and freshness remain visible.
- `MEASURED`: the connected radio repeatedly observed energy above the configured
  threshold. Protocol and user identity still require frame-level confirmation.

## Provider policy

| Provider | Region | Access | Intended use |
| --- | --- | --- | --- |
| NOAA Weather Radio | US | Open official reference | Exact weather channels; installed now |
| FCC ULS | US | Open official bulk files | Nearby licensed frequencies and sites |
| ISED TAFL | Canada | Open official extract | Nearby authorized frequencies and sites |
| ACMA RRL | Australia | Open official download | Nearby assignments; derivative terms apply |
| Ofcom SIS | UK | Open official files | Allocations and published licensing data |
| RadioReference | Multiple | Approved app key and user account | Optional trunking and talkgroup enrichment |
| Local spectrum survey | Receiver location | No account | Activity evidence measured by the user's SDR |

OpenMHz is not used as a directory API. Its published terms say that its API is
for its own website and iOS application unless permission is granted.

## Maintainability rules

1. Provider adapters perform download/parse only; they do not touch JavaFX.
2. `FrequencyDatabase` owns schema migration and SQL only.
3. Domain records cross module boundaries; JDBC rows and provider payloads do not.
4. UI panes render state and emit user intent; orchestration belongs in focused
   controllers/services as the early-access application class is decomposed.
5. No mixins. Prefer composition, narrow interfaces, immutable records, and
   constructor-injected services.
6. Every adapter needs fixture tests, source/terms documentation, explicit
   confidence mapping, update-size disclosure, and failure-safe offline behavior.
7. Initial multi-hundred-megabyte regulator archives require informed user action;
   subsequent differential updates may be automatic.

The desktop build contains a class-size ratchet: new desktop classes are capped at
300 lines, and the legacy application shell cannot grow past its current reduced
size. Each extraction lowers that one exceptional ceiling until the shell is a
small composition root. This is a guardrail rather than a claim that the remaining
shell is already ideal.

The schema uses a compound `(source_id, external_id)` key, transactional upserts,
WAL journaling, frequency/location indexes, and `PRAGMA user_version` migrations.
This lets adapters evolve independently and makes the cache inspectable with
ordinary SQLite tools.
