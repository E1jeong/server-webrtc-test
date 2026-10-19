# Signaling Server Module Guide

## Scope

- Central WebSocket signaling relay server for UBio WebRTC communication between `operator` and `device` peers.
- Owns HTTP health endpoint (`GET /health`), Docker Compose runtime, and stale connection heartbeat management.

## Orient First

- **Core Sources**:
  - Main server logic: [`src/server.js`](src/server.js)
  - Automated tests: [`test/server.test.js`](test/server.test.js)
  - Container setup: [`compose.yaml`](compose.yaml), [`Dockerfile`](Dockerfile)

## Boundary & Architecture Constraints

- **In-Memory Registry**: Single-process in-memory peer map (`peers`); no Redis or database integration.
- **Relay Only**: The server NEVER parses, inspects, or processes media tracks (audio/video).
- **Payload Safety**: Max WebSocket payload size is strictly 1 MiB.
- **Header & Stamp Injection**: The server overwrites the `from` field with the verified sender's Peer ID and injects `serverTimestamp`.
- **Minimal Logging**: Do NOT log raw SDP or ICE payload bodies in stdout/logs.

## Change Gates

1. **Protocol Shape Stability**: Maintain exact JSON message shapes defined in `technical/architecture.md` (`type`, `to`, `from`, `payload`).
2. **Duplicate Peer ID Handling**: When a duplicate peer ID registers, disconnect the older socket gracefully before registering the new socket.
3. **No Hardcoded Real IPs**: Never use real LAN/WAN IP addresses in test cases or fixtures; use localhost or test placeholders.

## Verify

```powershell
cd signaling-server
# Run unit tests
npm test

```

- For requested container/runtime verification, use the `components/signaling-server` wiki runbook; unit tests do not establish deployed health.
