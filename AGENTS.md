# UBio WebRTC AI Guide

This document is the repository-local AI navigation aid, task router, and execution/safety guard for the `ubio-webrtc` monorepo.

---

## Start Here

- **Navigation Aid Only**: This guide owns routing, boundary constraints, and verification commands. It is not an archive of decisions, meeting notes, or theoretical explanations.
- **Obsidian Wiki SSOT**: The authoritative project knowledge base starts at `Dev/Project/Company/ubio-webrtc/README.md`. Resolve the machine-specific vault through `_meta/routing-tables.md`; do not search arbitrary filesystem copies or use hardcoded `file:///` links.
- **Mandatory Session Read Order**:
  1. `Dev/Project/Company/ubio-webrtc/README.md` (Project boundary & publication policy)
  2. `Dev/Project/Company/ubio-webrtc/handoff.md` (Current state, active starting point)
  3. Applicable schemas, from parent to nearest: `_meta/global-schema-rules.md` then `Dev/Project/Company/ubio-webrtc/schema.md`
  4. `Dev/Project/Company/ubio-webrtc/index.md` (Task-area routing)
  5. `Dev/Project/Company/ubio-webrtc/issues/needs-verification.md` only when the task touches uncertainty or an unresolved claim
- **Before Editing**: For multi-step or resumed implementation, ground the wiki context against the live code, propose `step → verify` checkpoints, and confirm them before editing.
- **Environment & Topology**: Windows development PC (`DESKTOP-PE3TPJN`). The monorepo contains the central signaling server, the validated KMP Desktop operator, and a frozen browser reference operator. Android terminal test client lives in a separate repository (`android-anti-spoofing-lab`).
- **Language & Communication**: Use English for code comments, commit messages, maintained Project-wiki pages, and AI operating guides (`AGENTS.md`, `CLAUDE.md`). Report work in the user's language unless they request otherwise.

---

## Product and Runtime/Pipeline Map

```mermaid
flowchart LR
    OW["Frozen Operator Web Reference\n(:3019 Vinext/React 19)"]
    KO["KMP Desktop Operator\n(Compose Desktop)"]
    SS["Signaling Server\n(:8080 Node.js /ws)"]
    AD["Android Test Device\n(External Repo)"]

    OW <-->|"JSON WebSocket"| SS
    KO <-->|"JSON WebSocket"| SS
    AD <-->|"JSON WebSocket"| SS
    OW <-.->|"Reference comparison only"| AD
    KO <-->|"P2P WebRTC Audio/Video\n(Direct LAN)"| AD
```

---

## Module/Domain Map and First Reads

| Module | Purpose / Technology | First Source Entrypoint | Submodule Guide | Related Wiki Topic |
| --- | --- | --- | --- | --- |
| `signaling-server/` | Central WebSocket signaling relay (Node.js 24, `ws`, Docker) | [`signaling-server/src/server.js`](signaling-server/src/server.js) | [`signaling-server/AGENTS.md`](signaling-server/AGENTS.md) | `components/signaling-server.md` |
| `operator-web/` | Frozen browser reference client (React 19, Vinext/Vite, Tailwind); no active feature or UI development | [`operator-web/app/page.tsx`](operator-web/app/page.tsx) | [`operator-web/AGENTS.md`](operator-web/AGENTS.md) | `components/operator-web.md` |
| `kmp-operator/` | Active Kotlin Multiplatform Windows Desktop operator development | [`kmp-operator/desktopApp/src/main/kotlin/com/sumas/operator/main.kt`](kmp-operator/desktopApp/src/main/kotlin/com/sumas/operator/main.kt) | [`kmp-operator/AGENTS.md`](kmp-operator/AGENTS.md) | `technical/kmp-operator-migration-plan.md` |

---

## Task Router

| Developer Intent | First Wiki Page | Primary Source Entrypoint | Downstream Trace Path |
| --- | --- | --- | --- |
| Update signaling protocol / event format | `technical/architecture.md` | [`signaling-server/src/server.js`](signaling-server/src/server.js) | [`kmp-operator/shared/.../SignalingMessage.kt`](kmp-operator/shared/src/commonMain/kotlin/com/sumas/operator/signaling/SignalingMessage.kt) |
| Compare against the frozen browser reference | `components/operator-web.md` | [`operator-web/app/page.tsx`](operator-web/app/page.tsx) | [`operator-web/tests/rendered-html.test.mjs`](operator-web/tests/rendered-html.test.mjs) |
| Develop or verify the KMP Desktop operator | `technical/kmp-operator-migration-plan.md` | [`kmp-operator/shared/.../OperatorReducer.kt`](kmp-operator/shared/src/commonMain/kotlin/com/sumas/operator/state/OperatorReducer.kt) | [`kmp-operator/desktopApp/.../DesktopOperatorManager.kt`](kmp-operator/desktopApp/src/main/kotlin/com/sumas/operator/state/DesktopOperatorManager.kt) |
| Package standalone Windows distribution / MSI | `technical/kmp-operator-migration-plan.md` | [`kmp-operator/desktopApp/build.gradle.kts`](kmp-operator/desktopApp/build.gradle.kts) | [`kmp-operator/desktopApp/src/main/kotlin/com/sumas/operator/main.kt`](kmp-operator/desktopApp/src/main/kotlin/com/sumas/operator/main.kt) |
| Docker / Signaling deployment setup | `components/signaling-server.md` | [`signaling-server/compose.yaml`](signaling-server/compose.yaml) | [`signaling-server/Dockerfile`](signaling-server/Dockerfile) |
| Investigate unverified audio/video/network issues | `issues/needs-verification.md` | [`kmp-operator/desktopApp/.../DesktopOperatorManager.kt`](kmp-operator/desktopApp/src/main/kotlin/com/sumas/operator/state/DesktopOperatorManager.kt) | Physical device test log in wiki |

---

## Immutable Boundaries and Change Gates

1. **Secret & Sensitive Data Boundary**: Never commit or log real internal IPs, URLs, TURN credentials, tokens, or device/customer identifiers.
2. **Port 3019 on Windows Dev PC**: Default port 3000 conflicts on Windows development environments; `operator-web` MUST always run on `--port 3019 --hostname 127.0.0.1`.
3. **Source Code Preservation (`operator-web/build/sites-vite-plugin.ts`)**: `build/sites-vite-plugin.ts` is required source code, NOT a disposable build artifact. Do not delete or clean it.
4. **Frozen Browser Reference**: `operator-web` is retained only for regression comparison. Do not modify its UI, WebRTC flow, dependencies, or configuration unless the user explicitly requests a reference-client change.
5. **KMP Active Development**: The KMP Desktop operator is the active implementation target. Its completed LAN verification establishes a baseline; continue feature development, bug fixes, packaging, and remaining verification within the KMP module.
6. **Separate Android Repository Boundary**: Android client code lives in `android-anti-spoofing-lab`; do not assume Android source files exist in this monorepo.
7. **No Arbitrary `npm audit fix`**: `operator-web` has 18 deferred dependency audit findings pending compatibility review; do not execute automatic audit fixes.

---

## Build and Verification

### 1. Signaling Server
```powershell
cd signaling-server
npm test
# Docker runtime test
docker compose up -d --build
docker compose ps
docker compose down
```

### 2. Operator Web
```powershell
cd operator-web
npm run lint
npm test
# Dev server on port 3019
npm run dev -- --port 3019 --hostname 127.0.0.1
```

### 3. KMP Operator
```powershell
cd kmp-operator
.\gradlew.bat test
.\gradlew.bat desktopApp:test
```
