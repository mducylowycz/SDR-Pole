# Privacy and data handling

SDR-Pole is local-first and contains no analytics, advertising, crash-reporting,
or background telemetry. Map tiles and explicitly requested package/directory
downloads disclose the operator's IP address to the named upstream service.

Precise listening locations, radio serial numbers, credentials, recordings and
decoded call metadata may be sensitive in an operational deployment. They are
stored locally and are excluded or redacted from support and audit output.
Organizations must define authorization, retention, access, export and secure
deletion rules before operational use. Future authenticated directory secrets
must use operating-system credential storage and must never enter logs or plain
preferences.
