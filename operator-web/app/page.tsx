"use client";

import { useEffect, useMemo, useRef, useState } from "react";

type ConnectionStatus =
  | "disconnected"
  | "connecting"
  | "connected"
  | "registered"
  | "error";

type Peer = {
  peerId: string;
  peerType: "device" | "operator";
};

type SignalingMessage = {
  type: string;
  peerId?: string;
  peerType?: Peer["peerType"];
  peers?: Peer[];
  from?: string;
  to?: string;
  callId?: string;
  code?: string;
  message?: string;
  sdp?: string;
  candidate?: string;
  sdpMid?: string;
  sdpMLineIndex?: number;
};

type EventLog = {
  id: number;
  time: string;
  direction: "SEND" | "RECV" | "INFO";
  payload: string;
};

const statusText: Record<ConnectionStatus, string> = {
  disconnected: "연결 안 됨",
  connecting: "연결 중",
  connected: "등록 중",
  registered: "온라인",
  error: "연결 오류",
};

export default function Home() {
  const socketRef = useRef<WebSocket | null>(null);
  const peerConnectionRef = useRef<RTCPeerConnection | null>(null);
  const localStreamRef = useRef<MediaStream | null>(null);
  const localVideoRef = useRef<HTMLVideoElement | null>(null);
  const remoteVideoRef = useRef<HTMLVideoElement | null>(null);
  const pendingIceRef = useRef<RTCIceCandidateInit[]>([]);
  const activeCallRef = useRef<{ peerId: string; callId: string } | null>(
    null,
  );
  const callCounterRef = useRef(0);
  const logIdRef = useRef(0);
  const [serverUrl, setServerUrl] = useState("ws://localhost:8080/ws");
  const [operatorId, setOperatorId] = useState("operator-test-01");
  const [status, setStatus] =
    useState<ConnectionStatus>("disconnected");
  const [peers, setPeers] = useState<Peer[]>([]);
  const [logs, setLogs] = useState<EventLog[]>([]);
  const [callStatus, setCallStatus] = useState("통화 대기");
  const [mediaReady, setMediaReady] = useState(false);
  const [microphoneMuted, setMicrophoneMuted] = useState(false);
  const [activeCall, setActiveCall] = useState<{
    peerId: string;
    callId: string;
  } | null>(null);

  const devices = useMemo(
    () => peers.filter((peer) => peer.peerType === "device"),
    [peers],
  );

  function appendLog(
    direction: EventLog["direction"],
    payload: unknown,
  ) {
    const text =
      typeof payload === "string" ? payload : JSON.stringify(payload);
    setLogs((current) =>
      [
        {
          id: ++logIdRef.current,
          time: new Date().toLocaleTimeString("ko-KR", { hour12: false }),
          direction,
          payload: text,
        },
        ...current,
      ].slice(0, 80),
    );
  }

  function sendMessage(message: SignalingMessage) {
    const socket = socketRef.current;
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      appendLog("INFO", "WebSocket이 연결되지 않았습니다.");
      return;
    }

    socket.send(JSON.stringify(message));
    appendLog("SEND", message);
  }

  function connect() {
    if (
      socketRef.current &&
      socketRef.current.readyState < WebSocket.CLOSING
    ) {
      return;
    }

    setStatus("connecting");
    appendLog("INFO", `${serverUrl} 연결을 시작합니다.`);

    const socket = new WebSocket(serverUrl);
    socketRef.current = socket;

    socket.onopen = () => {
      setStatus("connected");
      const registerMessage = {
        type: "register",
        peerId: operatorId,
        peerType: "operator" as const,
      };
      socket.send(JSON.stringify(registerMessage));
      appendLog("SEND", registerMessage);
    };

    socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data) as SignalingMessage;
        appendLog("RECV", message);

        if (message.type === "registered") {
          setStatus("registered");
          setPeers(message.peers ?? []);
          sendMessage({ type: "peer.list" });
        } else if (message.type === "peer.list") {
          setPeers(message.peers ?? []);
        } else if (
          message.type === "peer.online" &&
          message.peerId &&
          message.peerType
        ) {
          setPeers((current) => [
            ...current.filter((peer) => peer.peerId !== message.peerId),
            {
              peerId: message.peerId as string,
              peerType: message.peerType as Peer["peerType"],
            },
          ]);
        } else if (message.type === "peer.offline" && message.peerId) {
          setPeers((current) =>
            current.filter((peer) => peer.peerId !== message.peerId),
          );
        } else if (
          message.type === "call.accept" &&
          message.from &&
          message.callId
        ) {
          void startVideoCall(message.from, message.callId);
        } else if (
          message.type === "webrtc.answer" &&
          message.from &&
          message.callId &&
          message.sdp
        ) {
          void applyAnswer(message.from, message.callId, message.sdp);
        } else if (
          message.type === "webrtc.ice" &&
          message.from &&
          message.callId &&
          message.candidate &&
          message.sdpMid !== undefined &&
          message.sdpMLineIndex !== undefined
        ) {
          void applyRemoteIce(message.from, message.callId, {
            candidate: message.candidate,
            sdpMid: message.sdpMid,
            sdpMLineIndex: message.sdpMLineIndex,
          });
        } else if (
          message.type === "call.hangup" ||
          message.type === "call.reject"
        ) {
          closeVideoCall();
        } else if (message.type === "error") {
          setStatus("error");
        }
      } catch {
        appendLog("INFO", `해석할 수 없는 서버 메시지: ${event.data}`);
      }
    };

    socket.onerror = () => {
      setStatus("error");
      appendLog("INFO", "WebSocket 연결 오류가 발생했습니다.");
    };

    socket.onclose = (event) => {
      if (socketRef.current === socket) {
        socketRef.current = null;
      }
      setStatus("disconnected");
      setPeers([]);
      closeVideoCall();
      appendLog(
        "INFO",
        `연결 종료: ${event.code}${event.reason ? ` / ${event.reason}` : ""}`,
      );
    };
  }

  function disconnect() {
    const socket = socketRef.current;
    socketRef.current = null;
    socket?.close(1000, "operator disconnected");
    setStatus("disconnected");
    setPeers([]);
    closeVideoCall();
  }

  function invite(deviceId: string) {
    closeVideoCall();
    const callId = `call-${operatorId}-${++callCounterRef.current}`;
    setCurrentCall({ peerId: deviceId, callId });
    setCallStatus(`${deviceId} 응답 대기`);
    sendMessage({
      type: "call.invite",
      callId,
      to: deviceId,
    });
  }

  async function startVideoCall(deviceId: string, callId: string) {
    const activeCall = activeCallRef.current;
    if (
      !activeCall ||
      activeCall.peerId !== deviceId ||
      activeCall.callId !== callId ||
      peerConnectionRef.current
    ) {
      return;
    }

    try {
      setCallStatus("카메라·마이크 준비 중");
      const localStream = await navigator.mediaDevices.getUserMedia({
        video: { width: 640, height: 480 },
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });
      localStreamRef.current = localStream;
      setMediaReady(true);
      if (localVideoRef.current) {
        localVideoRef.current.srcObject = localStream;
      }

      const peerConnection = new RTCPeerConnection({ iceServers: [] });
      peerConnectionRef.current = peerConnection;
      peerConnection.onicecandidate = (event) => {
        if (!event.candidate) return;
        sendMessage({
          type: "webrtc.ice",
          to: deviceId,
          callId,
          candidate: event.candidate.candidate,
          sdpMid: event.candidate.sdpMid ?? "0",
          sdpMLineIndex: event.candidate.sdpMLineIndex ?? 0,
        });
      };
      peerConnection.ontrack = (event) => {
        if (!remoteVideoRef.current) return;
        remoteVideoRef.current.srcObject =
          event.streams[0] ?? new MediaStream([event.track]);
        setCallStatus("단말 미디어 수신 중");
      };
      peerConnection.onconnectionstatechange = () => {
        setCallStatus(`WebRTC ${peerConnection.connectionState}`);
      };
      localStream
        .getTracks()
        .forEach((track) => peerConnection.addTrack(track, localStream));

      const offer = await peerConnection.createOffer({
        offerToReceiveVideo: true,
        offerToReceiveAudio: true,
      });
      await peerConnection.setLocalDescription(offer);
      sendMessage({
        type: "webrtc.offer",
        to: deviceId,
        callId,
        sdp: offer.sdp,
      });
      setCallStatus("음성·영상 연결 중");
    } catch (error) {
      appendLog(
        "INFO",
        `통화 시작 실패: ${error instanceof Error ? error.message : String(error)}`,
      );
      hangUp();
    }
  }

  async function applyAnswer(deviceId: string, callId: string, sdp: string) {
    const activeCall = activeCallRef.current;
    const peerConnection = peerConnectionRef.current;
    if (
      !activeCall ||
      activeCall.peerId !== deviceId ||
      activeCall.callId !== callId ||
      !peerConnection
    ) {
      return;
    }

    await peerConnection.setRemoteDescription({ type: "answer", sdp });
    for (const candidate of pendingIceRef.current.splice(0)) {
      await peerConnection.addIceCandidate(candidate);
    }
  }

  async function applyRemoteIce(
    deviceId: string,
    callId: string,
    candidate: RTCIceCandidateInit,
  ) {
    const activeCall = activeCallRef.current;
    const peerConnection = peerConnectionRef.current;
    if (
      !activeCall ||
      activeCall.peerId !== deviceId ||
      activeCall.callId !== callId ||
      !peerConnection
    ) {
      return;
    }
    if (!peerConnection.remoteDescription) {
      pendingIceRef.current.push(candidate);
      return;
    }
    await peerConnection.addIceCandidate(candidate);
  }

  function hangUp() {
    const activeCall = activeCallRef.current;
    if (activeCall) {
      sendMessage({
        type: "call.hangup",
        to: activeCall.peerId,
        callId: activeCall.callId,
      });
    }
    closeVideoCall();
  }

  function toggleMicrophoneMuted() {
    setMicrophoneMuted((current) => {
      const next = !current;
      localStreamRef.current
        ?.getAudioTracks()
        .forEach((track) => {
          track.enabled = !next;
        });
      return next;
    });
  }

  function closeVideoCall() {
    peerConnectionRef.current?.close();
    peerConnectionRef.current = null;
    localStreamRef.current?.getTracks().forEach((track) => track.stop());
    localStreamRef.current = null;
    pendingIceRef.current = [];
    setCurrentCall(null);
    setMediaReady(false);
    setMicrophoneMuted(false);
    if (localVideoRef.current) localVideoRef.current.srcObject = null;
    if (remoteVideoRef.current) remoteVideoRef.current.srcObject = null;
    setCallStatus("통화 대기");
  }

  function setCurrentCall(call: { peerId: string; callId: string } | null) {
    activeCallRef.current = call;
    setActiveCall(call);
  }

  useEffect(() => {
    return () => {
      peerConnectionRef.current?.close();
      localStreamRef.current?.getTracks().forEach((track) => track.stop());
      socketRef.current?.close(1000, "page closed");
    };
  }, []);

  return (
    <main className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">UBio WebRTC PoC</p>
          <h1>Operator Console</h1>
        </div>
        <div className={`status status-${status}`} aria-live="polite">
          <span className="status-dot" />
          {statusText[status]}
        </div>
      </header>

      <section className="connection-panel" aria-labelledby="connection-title">
        <div className="section-heading">
          <div>
            <p className="section-kicker">01 · Signaling</p>
            <h2 id="connection-title">서버 연결</h2>
          </div>
          <p>기존 signaling 서버에 operator Peer로 등록합니다.</p>
        </div>

        <div className="connection-form">
          <label>
            <span>WebSocket URL</span>
            <input
              value={serverUrl}
              onChange={(event) => setServerUrl(event.target.value)}
              disabled={status !== "disconnected" && status !== "error"}
              spellCheck={false}
            />
          </label>
          <label>
            <span>Operator ID</span>
            <input
              value={operatorId}
              onChange={(event) => setOperatorId(event.target.value)}
              disabled={status !== "disconnected" && status !== "error"}
              spellCheck={false}
            />
          </label>
          <div className="connection-actions">
            {status === "disconnected" || status === "error" ? (
              <button
                className="button button-primary"
                onClick={connect}
                disabled={!serverUrl.trim() || !operatorId.trim()}
              >
                서버 연결
              </button>
            ) : (
              <button className="button button-secondary" onClick={disconnect}>
                연결 해제
              </button>
            )}
          </div>
        </div>
      </section>

      <div className="workspace-grid">
        <section className="video-panel" aria-labelledby="video-title">
          <div className="section-heading compact">
            <div>
              <p className="section-kicker">02 · Audio / Video</p>
              <h2 id="video-title">음성·영상 통화</h2>
            </div>
            <div className="call-actions">
              <span className="call-status">{callStatus}</span>
              <button
                className="button button-secondary"
                onClick={toggleMicrophoneMuted}
                disabled={!mediaReady}
              >
                {microphoneMuted ? "마이크 켜기" : "마이크 끄기"}
              </button>
              <button
                className="button button-danger"
                onClick={hangUp}
                disabled={!activeCall}
              >
                통화 종료
              </button>
            </div>
          </div>
          <div className="video-stage">
            <div className="remote-frame">
              <div className="video-label">단말</div>
              <video
                ref={remoteVideoRef}
                className="remote-video"
                autoPlay
              playsInline
            />
            <div className="remote-placeholder">단말 영상 대기</div>
              <div className="local-frame">
                <div className="video-label">운영자</div>
                <video
                  ref={localVideoRef}
                  className="local-video"
                  autoPlay
                  playsInline
                  muted
                />
              </div>
            </div>
          </div>
        </section>

        <div className="content-grid">
          <section className="devices-panel" aria-labelledby="devices-title">
            <div className="section-heading compact">
              <div>
                <p className="section-kicker">03 · Devices</p>
                <h2 id="devices-title">온라인 단말</h2>
              </div>
              <span className="count-badge">{devices.length}</span>
            </div>

            {devices.length === 0 ? (
              <div className="empty-state">
                <span className="empty-icon">D</span>
                <strong>등록된 단말이 없습니다</strong>
                <p>
                  Face Pro 앱과 Operator Console이 같은 signaling 서버에
                  연결됐는지 확인하세요.
                </p>
              </div>
            ) : (
              <ul className="device-list">
                {devices.map((device) => (
                  <li key={device.peerId} className="device-card">
                    <div className="device-identity">
                      <span className="device-mark">FP</span>
                      <div>
                        <strong>{device.peerId}</strong>
                        <span>Face Pro · Online</span>
                      </div>
                    </div>
                    <button
                      className="button button-call"
                      onClick={() => invite(device.peerId)}
                      disabled={
                        status !== "registered" ||
                        activeCall !== null
                      }
                    >
                      통화 요청
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="log-panel" aria-labelledby="log-title">
            <div className="section-heading compact">
              <div>
                <p className="section-kicker">04 · Events</p>
                <h2 id="log-title">Signaling 로그</h2>
              </div>
              <button className="text-button" onClick={() => setLogs([])}>
                지우기
              </button>
            </div>

            <div className="event-log" role="log" aria-live="polite">
              {logs.length === 0 ? (
                <p className="log-placeholder">
                  서버에 연결하면 메시지가 여기에 표시됩니다.
                </p>
              ) : (
                logs.map((log) => (
                  <div className="log-row" key={log.id}>
                    <span className="log-time">{log.time}</span>
                    <span className={`log-direction direction-${log.direction}`}>
                      {log.direction}
                    </span>
                    <code>{log.payload}</code>
                  </div>
                ))
              )}
            </div>
          </section>
        </div>
      </div>

      <footer>
        LAN audio/video PoC · STUN and TURN are disabled.
      </footer>
    </main>
  );
}
