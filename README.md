# UBio WebRTC Proof of Concept

This monorepo provides a LAN-only proof of concept for one-to-one WebRTC audio and video calls between the UBio Android test device and the Windows Desktop operator console.

The KMP Desktop operator is the active development client. Its end-to-end LAN call path, including bidirectional audio and video, has been verified with the Android test device. The browser operator remains in this repository only as a frozen reference implementation and must not receive feature or UI development unless explicitly requested.

## Components

- `signaling-server/` — Node.js WebSocket relay for peer registration, call control, SDP, and ICE messages.
- `kmp-operator/` — Kotlin Multiplatform Windows Desktop operator console, including the WebRTC media path and Windows packaging configuration.
- `operator-web/` — Frozen React browser reference client for regression comparison only; it is not under active development.

The Android test-device implementation is maintained separately in the `android-anti-spoofing-lab` repository.

## Current Scope

- Trusted-LAN, fixed test peers
- Operator-initiated calls with Android auto-accept
- SDP Offer/Answer and trickle ICE signaling
- Bidirectional video and audio
- Microphone mute and media-track cleanup
- KMP Desktop operator packaging for Windows

STUN/TURN, authentication, HTTPS/WSS, and production deployment policy are outside the current proof-of-concept scope. Long-running call stability, reconnection, audio-device selection, and remaining hardware edge cases still require verification.

## Run the Signaling Server

```powershell
cd signaling-server
npm install
npm test
npm start
```

To run it with Docker:

```powershell
cd signaling-server
docker compose up -d --build
```

## Run the KMP Desktop Operator

```powershell
cd kmp-operator
.\gradlew.bat :desktopApp:run
```

Run the KMP test suites with:

```powershell
cd kmp-operator
.\gradlew.bat test
.\gradlew.bat desktopApp:test
```

## Reference Browser Operator

`operator-web/` is retained only for behavior comparison and regression checks. Do not modify it for normal development work. If an explicit reference-client check is needed on the Windows development PC, it must run on `127.0.0.1:3019`:

```powershell
cd operator-web
npm test
npm run dev -- --port 3019 --hostname 127.0.0.1
```
