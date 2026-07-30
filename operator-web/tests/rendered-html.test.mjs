import assert from "node:assert/strict";
import test from "node:test";

async function render() {
  const workerUrl = new URL("../dist/server/index.js", import.meta.url);
  workerUrl.searchParams.set("test", `${process.pid}-${Date.now()}`);
  const { default: worker } = await import(workerUrl.href);

  return worker.fetch(
    new Request("http://localhost/", {
      headers: { accept: "text/html" },
    }),
    {
      ASSETS: {
        fetch: async () => new Response("Not found", { status: 404 }),
      },
    },
    {
      waitUntil() {},
      passThroughOnException() {},
    },
  );
}

test("renders the operator signaling console", async () => {
  const response = await render();
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") ?? "", /^text\/html\b/i);

  const html = await response.text();
  assert.match(html, /<title>UBio Operator Console<\/title>/i);
  assert.match(html, /Operator Console/);
  assert.match(html, /서버 연결/);
  assert.match(html, /온라인 단말/);
  assert.match(html, /마이크 끄기/);
  assert.match(html, /Signaling 로그/);
  assert.doesNotMatch(html, /codex-preview|react-loading-skeleton/i);
});
