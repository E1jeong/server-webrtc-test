# Operator Web Module Guide

## Scope

- Browser-based WebRTC operator peer built with React 19, Vinext/Vite, and Tailwind CSS.
- Connects to signaling server as `operator`, displays online devices, initiates 1:1 calls, and manages WebRTC audio/video communication.
- Owns operator UI (portrait device video, top-right operator PiP overlay, non-mirrored preview, microphone mute toggle, signaling event log).

## Orient First

- **Wiki SSOT**: `Dev/Project/Company/ubio-webrtc/components/operator-web.md`
- **Architecture**: `Dev/Project/Company/ubio-webrtc/technical/architecture.md`
- **Core Sources**:
  - Main operator UI & WebRTC hook: [`app/page.tsx`](app/page.tsx)
  - Global styles: [`app/globals.css`](app/globals.css)
  - Rendered HTML test: [`tests/rendered-html.test.mjs`](tests/rendered-html.test.mjs)
  - Custom Vite plugin: [`build/sites-vite-plugin.ts`](build/sites-vite-plugin.ts)

## Boundary & Architecture Constraints

- **Call Initiator**: Operator generates the SDP Offer (`webrtc.offer`) and handles Answer (`webrtc.answer`) and ICE candidates.
- **Media Constraints**: Requests echo cancellation, noise suppression, and auto gain control via `getUserMedia`.
- **Track Lifecycle**: Local microphone mute toggles track `enabled` state; all media tracks MUST be explicitly stopped on call hangup.
- **Video Layout**: Portrait aspect ratio for device video; self-preview in PiP overlay; neither stream is mirrored.

## Change Gates

1. **Port 3019 Execution**: Always run with `--port 3019 --hostname 127.0.0.1` due to Windows default port 3000 exclusions.
2. **Preserve `build/sites-vite-plugin.ts`**: This file in `build/` is required source code, not a build output. Never delete it.
3. **No Automatic `npm audit fix`**: 18 dependency audit findings are deferred pending compatibility review. Do not run destructive auto-fix commands.
4. **No Direct Android Code Modification**: Android device logic resides in the external `android-anti-spoofing-lab` repository.

## Verify

```powershell
cd operator-web
# Lint check
npm run lint

# Build and rendered HTML verification test
npm test

# Development server
npm run dev -- --port 3019 --hostname 127.0.0.1
```
