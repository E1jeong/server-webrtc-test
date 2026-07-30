import assert from "node:assert/strict";
import test from "node:test";
import { WebSocket } from "ws";
import { createSignalingServer } from "../src/server.js";

const silentLogger = {
  info() {},
  warn() {}
};

function connect(url) {
  return new Promise((resolve, reject) => {
    const socket = new WebSocket(url);
    socket.once("open", () => resolve(socket));
    socket.once("error", reject);
  });
}

function nextMessage(socket) {
  return new Promise((resolve, reject) => {
    socket.once("message", (data) => {
      try {
        resolve(JSON.parse(data.toString()));
      } catch (error) {
        reject(error);
      }
    });
    socket.once("error", reject);
  });
}

test("health endpoint reports an empty peer registry", async () => {
  const server = await createSignalingServer({
    host: "127.0.0.1",
    port: 0,
    logger: silentLogger
  });

  try {
    const response = await fetch(`http://127.0.0.1:${server.port}/health`);
    assert.equal(response.status, 200);
    const body = await response.json();
    assert.equal(body.status, "ok");
    assert.equal(body.peers, 0);
  } finally {
    await server.close();
  }
});

test("registered peers can relay a call invite", async () => {
  const server = await createSignalingServer({
    host: "127.0.0.1",
    port: 0,
    logger: silentLogger
  });
  const url = `ws://127.0.0.1:${server.port}/ws`;
  const operator = await connect(url);
  const device = await connect(url);

  try {
    operator.send(JSON.stringify({
      type: "register",
      peerId: "operator-1",
      peerType: "operator"
    }));
    assert.equal((await nextMessage(operator)).type, "registered");

    device.send(JSON.stringify({
      type: "register",
      peerId: "device-1",
      peerType: "device"
    }));
    assert.equal((await nextMessage(device)).type, "registered");
    assert.equal((await nextMessage(operator)).type, "peer.online");

    operator.send(JSON.stringify({
      type: "call.invite",
      callId: "call-1",
      to: "device-1"
    }));
    const invite = await nextMessage(device);
    assert.equal(invite.type, "call.invite");
    assert.equal(invite.callId, "call-1");
    assert.equal(invite.from, "operator-1");
    assert.equal(invite.to, "device-1");
  } finally {
    operator.close();
    device.close();
    await server.close();
  }
});
