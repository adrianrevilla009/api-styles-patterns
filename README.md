# api-styles-patterns

Seven small demos of common API styles and delivery patterns, each built around the same Orders domain, so you can compare how pagination, RPC, GraphQL batching, push, webhooks, BFFs and SOAP behave.

## What is inside

| Folder | What it shows | Run |
| --- | --- | --- |
| [`rest-pagination`](./rest-pagination) | Offset vs cursor pagination when a row is inserted between pages | `java Main.java` |
| [`grpc-streaming`](./grpc-streaming) | gRPC unary call with a deadline, plus a server stream (in-process) | `mvn -q compile exec:java` |
| [`graphql-dataloader`](./graphql-dataloader) | N+1 queries collapsed into one batch by a hand-written DataLoader | `java Main.java` |
| [`websocket-sse`](./websocket-sse) | Server-Sent Events pushing order status over one HTTP response | `java Main.java` |
| [`webhooks-signed`](./webhooks-signed) | HMAC-signed webhook with replay window and sender retries | `java Main.java` |
| [`bff`](./bff) | One mobile endpoint fanning out to two services and trimming the result | `java Main.java` |
| [`soap-jaxws-legacy`](./soap-jaxws-legacy) | A SOAP 1.1 request and response built by hand over HTTP | `java Main.java` |

## Prerequisites

- Java 21 (all folders except `grpc-streaming` run with the JDK alone: no build tool, no network).
- Maven and access to Maven Central for `grpc-streaming` (gRPC 1.68.1, protobuf 3.25.5).

## How to read it

Start with `rest-pagination`, which is the shortest, then follow the table. Each folder is standalone; run its command from inside the folder. Only `grpc-streaming` was not run end to end (see its README).
