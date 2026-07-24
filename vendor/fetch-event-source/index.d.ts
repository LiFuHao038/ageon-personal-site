export const EventStreamContentType: "text/event-stream"

export type EventSourceMessage = {
  data: string
  event: string
  id: string
  retry?: number
}

export type FetchEventSourceInit = RequestInit & {
  onopen?: (response: Response) => Promise<void>
  onmessage?: (message: EventSourceMessage) => void
  onclose?: () => void
  onerror?: (error: unknown) => number | null | undefined | void
  openWhenHidden?: boolean
  fetch?: typeof globalThis.fetch
}

export function fetchEventSource(input: RequestInfo | URL, init?: FetchEventSourceInit): Promise<void>
