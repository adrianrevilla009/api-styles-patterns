# webhooks-signed

`Main.java` runs a webhook receiver and a sender in one process: HMAC-SHA256 signatures, a replay window and retries with exponential backoff.

## Goal

Show how a receiver authenticates a webhook and how a sender recovers from transient failures.

## Run it

```
cd webhooks-signed
java Main.java
```

Expected output:

```
forged=401 final=200 attempts=3
OK
```

## What it proves

- The signature is HMAC-SHA256 over `timestamp.body`, sent in `X-Signature` and `X-Timestamp`. A request signed `deadbeef` is rejected with 401.
- The receiver compares signatures with `MessageDigest.isEqual` (constant time) and rejects timestamps more than 300 s from now.
- For valid requests the receiver answers 503 twice, then 200; the sender retries with a 20 ms backoff that doubles each time and succeeds on attempt 3.

## Trade-offs

- The secret is a constant in `Main.java` for the demo; real senders and receivers load it from configuration.
- The replay window limits old requests but does not stop a replay within 5 minutes; that needs an event id store.
- Backoff has no jitter and a tiny base delay, so it is not tuned for real traffic.

## When not to use it

- When the consumer is on a trusted private network and a queue or direct call is simpler.
- When you need guaranteed ordering or exactly-once delivery; webhooks give at-least-once.
