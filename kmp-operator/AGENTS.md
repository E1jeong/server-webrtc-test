# KMP Operator Module Guide

## Scope

- Kotlin Multiplatform study PoC implementing a Desktop (Windows) operator peer and shared signaling logic.
- Implements `shared/commonMain` pure signaling DTOs, connection/call state, and `OperatorReducer`.
- Implements Compose Desktop console UI (`desktopApp/`) and chunk-safe `DesktopWebSocketClient` (JDK 11+ HttpClient WebSocket).

## Orient First

- **Wiki SSOT**: `Dev/Project/Company/ubio-webrtc/technical/kmp-operator-migration-plan.md`
- **Reference Implementation**: [`operator-web/app/page.tsx`](../operator-web/app/page.tsx)
- **Core Sources**:
  - Shared Signaling DTOs: [`SignalingMessage.kt`](shared/src/commonMain/kotlin/com/sumas/operator/signaling/SignalingMessage.kt)
  - Call State & Reducer: [`OperatorReducer.kt`](shared/src/commonMain/kotlin/com/sumas/operator/state/OperatorReducer.kt)
  - Desktop Operator Manager: [`DesktopOperatorManager.kt`](desktopApp/src/main/kotlin/com/sumas/operator/state/DesktopOperatorManager.kt)
  - Desktop Media Controller: [`DesktopMediaController.kt`](desktopApp/src/main/kotlin/com/sumas/operator/media/DesktopMediaController.kt)
  - WebRTC Desktop Media Controller: [`WebrtcDesktopMediaController.kt`](desktopApp/src/main/kotlin/com/sumas/operator/media/webrtc/WebrtcDesktopMediaController.kt)
  - WebRTC Video Renderer Sink: [`WebrtcVideoRendererSink.kt`](desktopApp/src/main/kotlin/com/sumas/operator/media/webrtc/WebrtcVideoRendererSink.kt)
  - Video Stage Panel UI: [`VideoStagePanel.kt`](desktopApp/src/main/kotlin/com/sumas/operator/ui/components/VideoStagePanel.kt)
  - Desktop Console UI: [`OperatorConsoleScreen.kt`](desktopApp/src/main/kotlin/com/sumas/operator/ui/OperatorConsoleScreen.kt)
  - Entrypoint: [`main.kt`](desktopApp/src/main/kotlin/com/sumas/operator/main.kt)

## Boundary & Architecture Constraints

- **Pure `commonMain` Separation**: Keep WebRTC, camera/mic capture, video rendering, and platform native libraries completely OUT of `shared/commonMain`. Only serializable protocol models and deterministic call/UI states live in `commonMain`.
- **Media Boundary**: Media handling belongs in `desktopApp/media` behind a dedicated controller interface (`DesktopMediaController`).
- **Chunk-Safe WebSocket**: `DesktopWebSocketClient` buffers fragmented UTF-8 frames via `StringBuilder` before deserialization.

## Change Gates

1. **Protocol Parity**: Must accept and emit exact JSON shapes compatible with `signaling-server` without requiring backend protocol changes.
2. **Reference Client Preservation**: Do not alter `operator-web` behavior; treat `operator-web` as the baseline.
3. **No Local/IDE Artifacts**: Never commit `local.properties`, `.gradle`, `.idea`, or OS build caches.

## Verify

```powershell
cd kmp-operator
# Run shared common tests
.\gradlew.bat shared:allTests

# Run Desktop app unit and live socket integration tests
.\gradlew.bat desktopApp:test

# Run all tests
.\gradlew.bat test

# Build standalone Windows distribution (unpacked directory with embedded JRE & DLLs)
.\gradlew.bat desktopApp:createDistributable

# Build Windows MSI installer package
.\gradlew.bat desktopApp:packageMsi
```
