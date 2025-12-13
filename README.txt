
Source installation information for modders
-------------------------------------------
This code follows the Minecraft Forge installation methodology. It will apply
some small patches to the vanilla MCP source code, giving you and it access 
to some of the data and functions you need to build a successful mod.

Note also that the patches are built against "un-renamed" MCP source code (aka
SRG Names) - this means that you will not be able to read them directly against
normal code.

Setup Process:
==============================

Step 1: Open your command-line and browse to the folder where you extracted the zip file.

Step 2: You're left with a choice.
If you prefer to use Eclipse:
1. Run the following command: `./gradlew genEclipseRuns`
2. Open Eclipse, Import > Existing Gradle Project > Select Folder 
   or run `gradlew eclipse` to generate the project.

If you prefer to use IntelliJ:
1. Open IDEA, and import project.
2. Select your build.gradle file and have it import.
3. Run the following command: `./gradlew genIntellijRuns`
4. Refresh the Gradle Project in IDEA if required.

If at any point you are missing libraries in your IDE, or you've run into problems you can 
run `gradlew --refresh-dependencies` to refresh the local cache. `gradlew clean` to reset everything 
(this does not affect your code) and then start the process again.

Mapping Names:
=============================
By default, the MDK is configured to use the official mapping names from Mojang for methods and fields 
in the Minecraft codebase. These names are covered by a specific license. All modders should be aware of this
license, if you do not agree with it you can change your mapping names to other crowdsourced names in your 
build.gradle. For the latest license text, refer to the mapping file itself, or the reference copy here:
https://github.com/MinecraftForge/MCPConfig/blob/master/Mojang.md

Additional Resources: 
=========================
Community Documentation: https://docs.minecraftforge.net/en/1.20.1/gettingstarted/
LexManos' Install Video: https://youtu.be/8VEdtQLuLO0
Forge Forums: https://forums.minecraftforge.net/
Forge Discord: https://discord.minecraftforge.net/

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

Testing when a proxy blocks downloads
-------------------------------------
- A local Gradle 8.14.x installation is available in the environment, so use `gradle test` instead of `./gradlew test` to avoid the wrapper trying to download its own distribution through the proxy.
- External Maven and Gradle Plugin portals are blocked (403) in this environment, so dependency and plugin resolution will fail unless you provide a reachable mirror. Configure a proxy/mirror via `~/.gradle/gradle.properties` or pre-populate `~/.gradle/caches` and `~/.gradle/wrapper/dists` from a networked machine.
- Once dependencies are cached or a mirror is configured, rerun `gradle test` from the repo root.
