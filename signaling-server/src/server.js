import http from "node:http";
import { pathToFileURL } from "node:url";
import { WebSocket, WebSocketServer } from "ws";

const DEFAULT_PORT = 8080;
const DEFAULT_HOST = "0.0.0.0";
const MAX_MESSAGE_BYTES = 1024 * 1024;
const HEARTBEAT_INTERVAL_MS = 30_000;
const PEER_ID_PATTERN = /^[A-Za-z0-9._:-]{1,128}$/;
const RELAY_MESSAGE_TYPES = new Set([
  "call.invite",
  "call.accept",
  "call.reject",
  "webrtc.offer",
  "webrtc.answer",
  "webrtc.ice",
  "call.hangup"
]);

function send(socket, message) {
  if (socket.readyState === WebSocket.OPEN) {
    socket.send(JSON.stringify(message));
  }
}

function sendError(socket, code, message) {
  send(socket, { type: "error", code, message });
}

function publicPeer(peer) {
  return {
    peerId: peer.peerId,
    peerType: peer.peerType
  };
}

export async function createSignalingServer({
  host = DEFAULT_HOST,
  port = DEFAULT_PORT,
  logger = console
} = {}) {
  const peers = new Map();
  const httpServer = http.createServer((request, response) => {
    if (request.method === "GET" && request.url === "/health") {
      response.writeHead(200, { "content-type": "application/json; charset=utf-8" });
      response.end(JSON.stringify({
        status: "ok",
        peers: peers.size,
        uptimeSeconds: Math.floor(process.uptime())
      }));
      return;
    }

    response.writeHead(404, { "content-type": "application/json; charset=utf-8" });
    response.end(JSON.stringify({ error: "not_found" }));
  });

  const webSocketServer = new WebSocketServer({
    noServer: true,
    maxPayload: MAX_MESSAGE_BYTES,
    perMessageDeflate: false
  });

  function broadcast(message, excludedSocket = null) {
    for (const peer of peers.values()) {
      if (peer.socket !== excludedSocket) {
        send(peer.socket, message);
      }
    }
  }

  function removePeer(socket) {
    if (!socket.peerId) {
      return;
    }

    const current = peers.get(socket.peerId);
    if (current?.socket !== socket) {
      return;
    }

    peers.delete(socket.peerId);
    logger.info(`peer offline: ${socket.peerId}`);
    broadcast({
      type: "peer.offline",
      peerId: socket.peerId
    });
  }

  function registerPeer(socket, message) {
    const peerId = typeof message.peerId === "string" ? message.peerId.trim() : "";
    const peerType = typeof message.peerType === "string" ? message.peerType.trim() : "";

    if (!PEER_ID_PATTERN.test(peerId)) {
      sendError(socket, "invalid_peer_id", "peerId must be 1-128 letters, numbers, dots, underscores, colons, or hyphens.");
      return;
    }

    if (peerType !== "device" && peerType !== "operator") {
      sendError(socket, "invalid_peer_type", "peerType must be device or operator.");
      return;
    }

    if (socket.peerId) {
      sendError(socket, "already_registered", "This connection is already registered.");
      return;
    }

    const previous = peers.get(peerId);
    if (previous) {
      send(previous.socket, {
        type: "error",
        code: "peer_replaced",
        message: "A newer connection registered with the same peerId."
      });
      previous.socket.close(4001, "replaced");
    }

    socket.peerId = peerId;
    socket.peerType = peerType;
    peers.set(peerId, { peerId, peerType, socket });
    logger.info(`peer online: ${peerId} (${peerType})`);

    send(socket, {
      type: "registered",
      peerId,
      peers: [...peers.values()].map(publicPeer)
    });
    broadcast({ type: "peer.online", peerId, peerType }, socket);
  }

  function relayMessage(socket, message) {
    if (!socket.peerId) {
      sendError(socket, "not_registered", "Register this connection before sending messages.");
      return;
    }

    if (message.type === "peer.list") {
      send(socket, {
        type: "peer.list",
        peers: [...peers.values()].map(publicPeer)
      });
      return;
    }

    if (!RELAY_MESSAGE_TYPES.has(message.type)) {
      sendError(socket, "unsupported_type", "Unsupported message type.");
      return;
    }

    const targetId = typeof message.to === "string" ? message.to.trim() : "";
    const target = peers.get(targetId);
    if (!target) {
      sendError(socket, "peer_offline", `Target peer is not connected: ${targetId}`);
      return;
    }

    const relayedMessage = {
      ...message,
      from: socket.peerId,
      to: targetId,
      serverTimestamp: new Date().toISOString()
    };
    send(target.socket, relayedMessage);
    logger.info(`relay ${message.type}: ${socket.peerId} -> ${targetId}`);
  }

  webSocketServer.on("connection", (socket) => {
    socket.isAlive = true;
    socket.on("pong", () => {
      socket.isAlive = true;
    });

    socket.on("message", (data, isBinary) => {
      if (isBinary) {
        sendError(socket, "binary_not_supported", "Only JSON text messages are supported.");
        return;
      }

      let message;
      try {
        message = JSON.parse(data.toString());
      } catch {
        sendError(socket, "invalid_json", "Message must be valid JSON.");
        return;
      }

      if (!message || typeof message !== "object" || Array.isArray(message)) {
        sendError(socket, "invalid_message", "Message must be a JSON object.");
        return;
      }

      if (message.type === "register") {
        registerPeer(socket, message);
        return;
      }

      relayMessage(socket, message);
    });

    socket.on("close", () => removePeer(socket));
    socket.on("error", (error) => {
      logger.warn(`websocket error${socket.peerId ? ` (${socket.peerId})` : ""}: ${error.message}`);
    });
  });

  httpServer.on("upgrade", (request, socket, head) => {
    const url = new URL(request.url ?? "/", "http://localhost");
    if (url.pathname !== "/ws") {
      socket.write("HTTP/1.1 404 Not Found\r\nConnection: close\r\n\r\n");
      socket.destroy();
      return;
    }

    webSocketServer.handleUpgrade(request, socket, head, (webSocket) => {
      webSocketServer.emit("connection", webSocket, request);
    });
  });

  const heartbeat = setInterval(() => {
    for (const socket of webSocketServer.clients) {
      if (!socket.isAlive) {
        socket.terminate();
        continue;
      }
      socket.isAlive = false;
      socket.ping();
    }
  }, HEARTBEAT_INTERVAL_MS);
  heartbeat.unref();

  await new Promise((resolve, reject) => {
    httpServer.once("error", reject);
    httpServer.listen(port, host, () => {
      httpServer.off("error", reject);
      resolve();
    });
  });

  const address = httpServer.address();
  logger.info(`signaling server listening on ${typeof address === "object" ? `${address.address}:${address.port}` : address}`);

  return {
    host,
    port: typeof address === "object" ? address.port : port,
    close: async () => {
      clearInterval(heartbeat);
      for (const socket of webSocketServer.clients) {
        socket.close(1001, "server shutdown");
      }
      await new Promise((resolve) => webSocketServer.close(resolve));
      await new Promise((resolve, reject) => {
        httpServer.close((error) => error ? reject(error) : resolve());
      });
    }
  };
}

const isMainModule = process.argv[1]
  && import.meta.url === pathToFileURL(process.argv[1]).href;

if (isMainModule) {
  const port = Number.parseInt(process.env.PORT ?? `${DEFAULT_PORT}`, 10);
  const host = process.env.HOST ?? DEFAULT_HOST;
  const server = await createSignalingServer({ host, port });

  async function shutdown(signal) {
    console.info(`received ${signal}, shutting down`);
    await server.close();
    process.exit(0);
  }

  process.once("SIGINT", () => void shutdown("SIGINT"));
  process.once("SIGTERM", () => void shutdown("SIGTERM"));
}
