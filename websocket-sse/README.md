# websocket-sse

`Main.java` starts a JDK `HttpServer` that streams order status events as Server-Sent Events, and reads them with the JDK `HttpClient`.

## Goal

Show server push over plain HTTP: one long-lived response carrying `text/event-stream` events. Despite the folder name, only SSE is implemented; there is no WebSocket code.

## Run it

```
cd websocket-sse
java Main.java
```

Expected output:

```
content-type=text/event-stream events=[CREATED, PAID, SHIPPED]
OK
```

## What it proves

- `/orders/events` answers with `Content-Type: text/event-stream` and flushes three `event: status` frames, 50 ms apart.
- The client reads the response line by line and extracts the `data: ` lines in order.
- No special protocol upgrade is needed: it is a normal HTTP response that stays open.

## Trade-offs

- SSE is one-way (server to client). Anything bidirectional needs WebSocket, which is not shown here.
- The demo has no `id:` field, reconnection handling or `Last-Event-ID` replay.
- Each open stream holds a connection and a handler thread on the server.

## When not to use it

- When the client must send frequent messages back on the same channel (chat, games).
- When updates are rare and polling or a webhook is simpler.
