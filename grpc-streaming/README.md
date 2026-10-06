# grpc-streaming

A Maven project with `orders.proto` and `Demo.java`: an in-process gRPC server and client with a unary call, a deadline and a server stream.

## Goal

Show gRPC unary and server-streaming calls, and how a client deadline turns a slow handler into a `DEADLINE_EXCEEDED` error.

## Run it

```
cd grpc-streaming
mvn -q compile exec:java
```

Expected output (not run here, taken from what `Demo.java` prints):

```
unary=SHIPPED deadline=DEADLINE_EXCEEDED streamed=3
OK
```

Not run end to end: it needs Maven Central to download gRPC, protoc and its plugin, and that was not available. The code is unverified; the other six folders were run.

## What it proves

- `Orders.Get` is a unary RPC; order 7 returns `SHIPPED`, order 99 sleeps 500 ms in the handler.
- A 50 ms deadline on the order 99 call should fail with `DEADLINE_EXCEEDED`, while a 1 s deadline on order 7 succeeds.
- `Orders.Watch` is a server stream; with `count=3` the client iterator should yield 3 `Order` messages.

## Trade-offs

- The in-process transport skips the network, TLS and real serialization over HTTP/2.
- Only server streaming is shown; no client or bidirectional streaming.
- Versions are pinned (gRPC 1.68.1, protobuf 3.25.5) and code is generated from the `.proto` at build time, so a build needs protoc binaries for your platform.

## When not to use it

- When browsers or third parties are the main clients; REST is easier to consume.
- For tiny, infrequent payloads where a generated-stub toolchain is overkill.
