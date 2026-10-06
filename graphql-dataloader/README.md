# graphql-dataloader

`Main.java` models resolving `{ orders { customer { name } } }` for 10 orders, first with one lookup per order and then through a small hand-written DataLoader. No GraphQL library is used.

## Goal

Show the N+1 query problem and how a DataLoader batches the keys requested in one tick into a single fetch.

## Run it

```
cd graphql-dataloader
java Main.java
```

Expected output:

```
naive queries=10 dataloader queries=1
OK
```

## What it proves

- The naive loop calls `fetchCustomers` once per order: 10 "queries" for 10 orders.
- The `DataLoader` class queues keys in `load`, and `dispatch` makes one batched call; the repeated customer ids (1, 2, 3) collapse because `pending` is keyed by id.
- Each `CompletableFuture` returned by `load` completes with its own customer after the single dispatch.

## Trade-offs

- The loader here is manual: `dispatch` is called by the demo. Real libraries (java-dataloader, graphql-java) dispatch automatically at the end of a tick.
- No per-request cache or batch size limit; very large key sets would need chunking.
- It simulates the database with a counter, so it proves the query count, not latency.

## When not to use it

- When a resolver already fetches related data in a join, batching adds nothing.
- Single-object lookups where there is no fan-out to collapse.
