import assert from "node:assert/strict"
import { fetchEventSource } from "../vendor/fetch-event-source/index.js"

const encoder = new TextEncoder()
const chunks = [
  "event: message\ndata: {\"delta\":\"TC",
  "P\"}\n\nevent: note\ndata: first\ndata: second\n\n",
]
const received = []
let request

await fetchEventSource("http://localhost/stream", {
  method: "POST",
  headers: { Authorization: "Bearer test-token" },
  body: JSON.stringify({ content: "question" }),
  fetch: async (_input, init) => {
    request = init
    return new Response(new ReadableStream({
      start(controller) {
        for (const chunk of chunks) controller.enqueue(encoder.encode(chunk))
        controller.close()
      },
    }), { status: 200, headers: { "Content-Type": "text/event-stream;charset=UTF-8" } })
  },
  onmessage(message) {
    received.push(message)
  },
  onerror(error) {
    throw error
  },
})

assert.equal(request.method, "POST")
assert.equal(request.headers.Authorization, "Bearer test-token")
assert.deepEqual(received, [
  { event: "message", data: "{\"delta\":\"TCP\"}", id: "" },
  { event: "note", data: "first\nsecond", id: "" },
])

console.log("fetch-event-source checks passed")
