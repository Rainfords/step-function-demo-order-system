declare module 'sockjs-client' {
  class SockJS extends EventTarget {
    constructor(url: string);
    send(data: string): void;
    close(): void;
    onopen: (() => void) | null;
    onmessage: ((event: MessageEvent) => void) | null;
    onerror: ((error: Event) => void) | null;
    onclose: (() => void) | null;
    readyState: number;
  }
  export = SockJS;
}

declare module 'stompjs' {
  export interface Client {
    connect(
      headers: Record<string, string>,
      connectCallback: () => void,
      errorCallback: (error: unknown) => void
    ): void;
    subscribe(
      destination: string,
      callback: (message: { body: string }) => void
    ): { id: string; unsubscribe: () => void };
    disconnect(callback: () => void): void;
    connected: boolean;
  }

  export function over(socket: WebSocket): Client;
}
