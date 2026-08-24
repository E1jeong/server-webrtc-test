# Signaling Server Module Guide

## Scope

- Central WebSocket signaling relay server for UBio WebRTC communication between `operator` and `device` peers.
- Relays registration, presence (`peer.list`, `peer.online`, `peer.offline`), call negotiation (`call.invite`, `call.accept`, `call.reject`, `call.hangup`), and WebRTC signaling (`webrtc.offer`, `webrtc.answer`, `webrtc.ice`).
- Owns HTTP health endpoint (`GET /health`), Docker Compose runtime, and stale connection heartbeat management.

## Orient First

- **Wiki SSOT**: `Dev/Project/Company/ubio-webrtc/components/signaling-server.md`
- **Architecture & Protocol**: `Dev/Project/Company/ubio-webrtc/technical/architecture.md`
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

# Docker container build & health check
docker compose up -d --build
docker compose ps
docker compose logs -f signaling
docker compose down
```
