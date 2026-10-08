# UBio WebRTC Signaling Server

Android 테스트 단말과 운영자 클라이언트 사이에서 WebRTC 연결 정보만 전달하는 회사 LAN 내부 PoC 서버입니다. 영상과 음성은 이 서버를 통과하지 않습니다. 활성 운영자 클라이언트는 KMP Desktop이고, 브라우저 운영자는 회귀 비교용으로만 남아 있습니다.

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

같은 PC의 운영자 클라이언트:

```text
ws://localhost:8080/ws
```

Android 테스트 단말:

```text
ws://<회사-PC-LAN-IP>:8080/ws
```

단말에서 접속하려면 호스트의 TCP 8080이 그 LAN에서 도달할 수 있어야 합니다.

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
  "peers": [
    { "peerId": "device-1", "peerType": "device" }
  ]
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
npm start
```
