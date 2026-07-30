# UBio WebRTC Signaling Server

Face Pro와 관리자 웹 사이에서 WebRTC 연결 정보만 전달하는 회사 LAN 내부 PoC 서버입니다. 영상과 음성은 이 서버를 통과하지 않습니다.

## 제공 기능

- `GET /health`: 서버 상태와 접속 Peer 수
- `WS /ws`: JSON WebSocket
- `device` 및 `operator` Peer 등록
- 온라인 Peer 목록과 접속·종료 알림
- 통화 요청·수락·거절·종료 메시지 중계
- WebRTC SDP Offer/Answer와 ICE Candidate 중계
- 끊긴 WebSocket 정리를 위한 heartbeat

인증, TLS, 영속 저장소, 통화 상태 복구, TURN은 포함하지 않습니다. 현재 구성은 신뢰할 수 있는 회사 LAN의 기능 검증 전용입니다.

## 실행

```powershell
docker compose up -d --build
```

상태 확인:

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8080/health
docker compose logs -f signaling
```

종료:

```powershell
docker compose down
```

## WebSocket 접속

관리자 웹이 같은 PC에서 실행될 때:

```text
ws://localhost:8080/ws
```

Face Pro에서 접속할 때:

```text
ws://<회사-PC-LAN-IP>:8080/ws
```

Windows 방화벽에서는 TCP 8080 인바운드를 Face Pro가 속한 네트워크 범위에만 허용합니다.

## 메시지 흐름

접속 직후 먼저 등록합니다.

```json
{
  "type": "register",
  "peerId": "device-1",
  "peerType": "device"
}
```

서버가 등록 결과와 현재 Peer 목록을 응답합니다.

```json
{
  "type": "registered",
  "peerId": "device-1",
  "peers": []
}
```

다른 Peer에게 전달하는 메시지는 `to`를 포함합니다.

```json
{
  "type": "call.invite",
  "callId": "call-1",
  "to": "device-1"
}
```

지원하는 중계 타입:

- `call.invite`
- `call.accept`
- `call.reject`
- `webrtc.offer`
- `webrtc.answer`
- `webrtc.ice`
- `call.hangup`

현재 온라인 목록을 다시 요청할 수도 있습니다.

```json
{
  "type": "peer.list"
}
```

## 로컬 테스트

Docker 이미지 빌드 시 테스트가 자동 실행되며, 실패하면 이미지가 만들어지지 않습니다.

Node.js를 로컬에 설치한 경우에는 직접 실행할 수도 있습니다.

```powershell
npm install
npm test
```
