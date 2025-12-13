
Telemetry
---------
Use `/telemetry json test123` on the server to emit a single-line TELEMETRY log entry containing the current player list.

Example output:
`TELEMETRY test123 {"mc":"1.20.1","loader":"forge","mspt":12.3,"tps":20.0,"players":[{"name":"Steve","uuid":"00000000000000000000000000000000"}]}`

Local HTTP endpoint: http://127.0.0.1:8765/telemetry
------------------------------------------------------
- The mod hosts a lightweight local HTTP server bound to 127.0.0.1 by default.
- Fetch the most recent telemetry JSON without spamming the console:
  - `curl http://127.0.0.1:8765/telemetry`
- Health check: `curl http://127.0.0.1:8765/health`
- Configuration:
  - `httpPort` (or system property `MCTELEMETRY_PORT`) controls the port, default `8765`.
  - `httpBindAddress` controls the bind address (default `127.0.0.1`, loopback only). Use `0.0.0.0` when running inside Docker/Pterodactyl. An override can also be set via system property `MCTELEMETRY_BIND`.
  - `telemetryRefreshTicks` controls how often the cached telemetry JSON is refreshed on the server thread (default `200`).

Project layout
--------------
- `common/`: Platform-agnostic telemetry payload builder shared across loaders.
- `forge/`: Forge entrypoint and command wiring that depends on the shared telemetry core.

