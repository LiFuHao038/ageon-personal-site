export const EventStreamContentType = "text/event-stream"

export async function fetchEventSource(input, init = {}) {
  const {
    onopen,
    onmessage,
    onclose,
    onerror,
    openWhenHidden: _openWhenHidden,
    fetch: fetchImplementation = globalThis.fetch,
    ...requestInit
  } = init

  while (true) {
    try {
      const response = await fetchImplementation(input, requestInit)
      if (onopen) {
        await onopen(response)
      } else if (!response.ok || !response.headers.get("content-type")?.includes(EventStreamContentType)) {
        throw new Error(`Unexpected response: ${response.status}`)
      }
      if (!response.body) throw new Error("Response body is unavailable")
      await readEventStream(response.body, onmessage)
      onclose?.()
      return
    } catch (error) {
      if (requestInit.signal?.aborted) throw error
      if (!onerror) throw error
      const retryDelay = onerror(error)
      if (retryDelay == null) return
      await delay(retryDelay, requestInit.signal)
    }
  }
}

async function readEventStream(stream, onmessage) {
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  let buffer = ""
  let eventName = ""
  let eventId = ""
  let eventData = []

  const dispatch = () => {
    if (eventData.length === 0) return
    onmessage?.({ data: eventData.join("\n"), event: eventName, id: eventId })
    eventName = ""
    eventData = []
  }

  while (true) {
    const { value, done } = await reader.read()
    buffer += decoder.decode(value, { stream: !done })
    const lines = buffer.split(/\r?\n/)
    buffer = done ? "" : lines.pop() ?? ""
    for (const line of lines) {
      if (line === "") {
        dispatch()
        continue
      }
      if (line.startsWith(":")) continue
      const separator = line.indexOf(":")
      const field = separator < 0 ? line : line.slice(0, separator)
      const rawValue = separator < 0 ? "" : line.slice(separator + 1)
      const fieldValue = rawValue.startsWith(" ") ? rawValue.slice(1) : rawValue
      if (field === "event") eventName = fieldValue
      if (field === "data") eventData.push(fieldValue)
      if (field === "id") eventId = fieldValue
    }
    if (done) {
      dispatch()
      return
    }
  }
}

function delay(milliseconds, signal) {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(resolve, milliseconds)
    signal?.addEventListener("abort", () => {
      clearTimeout(timeout)
      reject(signal.reason)
    }, { once: true })
  })
}
