# UBio WebRTC AI Guide

## Context

- Governs code navigation, component boundaries, and execution safety for the `ubio-webrtc` monorepo.
- The Obsidian wiki at vault-relative `Dev/Project/Company/ubio-webrtc` is the single source of truth for architecture, message protocols, deployment topology, and component verification. Resolve the vault through `_meta/routing-tables.md` or `obsidian-wiki-sync`, never a hardcoded file URL.
- **External Boundaries**: Android test client lives in a separate repository (`android-anti-spoofing-lab`); UBio-N Face Pro production integration remains deferred.
- **Environment**: Windows development PC (`DESKTOP-PE3TPJN`). The monorepo houses the central signaling server, active KMP Desktop operator, and frozen browser reference operator.
- Before multi-step or resumed implementation, ground the wiki context against live code, propose `step → verify` checkpoints, and confirm them before editing.
- Report to the user in Korean; keep code, identifiers, paths, and commands in English.
- Read the nearest component `AGENTS.md` before modifying a submodule; this root guide remains in force everywhere.

## Code Map

| Module | Responsibility | First entry point | Module guide |
| --- | --- | --- | --- |
| `signaling-server/` | Central WebSocket signaling relay (Node.js 24, `ws`, Docker) | `signaling-server/src/server.js` | `signaling-server/AGENTS.md` |
| `operator-web/` | Frozen browser reference client (React 19, Vite, Tailwind); regression comparison only | `operator-web/app/page.tsx` | `operator-web/AGENTS.md` |
| `kmp-operator/` | Active Kotlin Multiplatform Windows Desktop operator development | `kmp-operator/desktopApp/src/main/kotlin/com/sumas/operator/main.kt` | `kmp-operator/AGENTS.md` |

## Change Gates

- **Secret & Sensitive Data Boundary**: Never commit or log real internal IPs, URLs, TURN credentials, tokens, or device/customer identifiers.
- **Port 3019 on Windows Dev PC**: Port 3000 conflicts on Windows development environments; `operator-web` MUST always run on `--port 3019 --hostname 127.0.0.1`.
- **Source Code Preservation (`operator-web/build/sites-vite-plugin.ts`)**: `build/sites-vite-plugin.ts` is required source code, NOT a disposable build artifact. Do not delete or clean it.
- **Frozen Browser Reference**: `operator-web` is retained only for regression comparison. Do not modify its UI, WebRTC flow, dependencies, or configuration unless explicitly requested.
- **KMP Active Development**: The KMP Desktop operator is the active implementation target. Continue feature development, bug fixes, packaging, and verification within `kmp-operator`.
- **Separate Android Repository Boundary**: Android client code lives in `android-anti-spoofing-lab`; do not assume Android source files exist in this monorepo.
- **No Arbitrary `npm audit fix`**: `operator-web` has deferred dependency audit findings pending compatibility review; do not execute automatic audit fixes.

## Verify

- **Signaling Server**:
  ```powershell
  cd signaling-server; npm test
  ```
- **Operator Web**:
  ```powershell
  cd operator-web; npm run lint; npm test
  ```
- **KMP Operator**:
  ```powershell
  cd kmp-operator; .\gradlew.bat test; .\gradlew.bat desktopApp:test
  ```
- Report exact commands and results. Never commit or push: the user manages all git commits and pushes manually.
