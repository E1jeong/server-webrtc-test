# UBio WebRTC PoC

UBio 단말과 operator browser 사이의 1:1 WebRTC 통화를 검증하는 LAN 전용 모노레포입니다.

## 구성

- `signaling-server/`: Peer 등록, 통화 제어, SDP/ICE 중계를 담당하는 Node.js WebSocket 서버
- `operator-web/`: 온라인 단말 목록, 통화 UI와 browser WebRTC Peer를 제공하는 vinext 웹

Android 단말 구현은 별도 `android-anti-spoofing-lab` 저장소에서 관리합니다.

## 실행

### Signaling server

```powershell
cd signaling-server
npm install
npm test
npm start
```

Docker로 실행하려면:

```powershell
cd signaling-server
docker compose up -d --build
```

### Operator web

```powershell
cd operator-web
npm install
npm test
npm run dev -- --port 3019 --hostname 127.0.0.1
```

Windows 개발 PC에서는 기본 3000번 포트가 제외 범위와 충돌할 수 있어 3019번을 사용합니다.

## 현재 범위

- 같은 신뢰 LAN의 고정 test Peer
- operator 발신, Android 자동 수락
- SDP Offer/Answer와 trickle ICE
- 양방향 영상·음성
- browser와 Android 마이크 음소거
- browser 미디어 제약 및 Android WebRTC 오디오 처리 기반 에코 제거·노이즈 억제
- Android 음성통화 오디오 포커스, 스피커 라우팅과 종료 시 복구

STUN/TURN, 인증, HTTPS/WSS와 운영 배포는 아직 구현하지 않았습니다. 실제 장비의 에코·하울링·볼륨·오디오 포커스 전환은 별도 검증이 필요합니다.
