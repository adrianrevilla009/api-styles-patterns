# bff

`Main.java` starts two downstream stub services and a backend-for-frontend endpoint `/mobile/order-summary` that combines them.

## Goal

Show a BFF that serves one screen-shaped response by calling an orders service and a customers service in parallel and dropping fields the screen does not need.

## Run it

```
cd bff
java Main.java
```

Expected output:

```
{"orderId":7,"total":42.50,"customer":"Ada"}
OK
```

## What it proves

- The BFF fires both downstream calls with `sendAsync` and joins them with `thenCombine`, so the client makes a single request.
- The response contains only `orderId`, `total` and the customer name; the customer email and the order's `internalNotes` never leave the BFF (the demo asserts the output does not contain `secret`).
- The downstream services are plain JDK `HttpServer` stubs on random local ports.

## Trade-offs

- The downstream bodies use a toy `key=value;` format and the JSON is built by string concatenation; real code would use a JSON library.
- Ids 7 and 3 are hard-coded in the BFF; there is no routing, auth, timeout or error handling for a failing downstream.
- Each client type tends to get its own BFF, which adds code to maintain.

## When not to use it

- With a single client whose needs match the existing API.
- When a GraphQL gateway already lets each client pick its fields.
