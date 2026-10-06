# rest-pagination

`Main.java` compares offset and cursor pagination over an in-memory Orders table while a new order arrives between two page reads.

## Goal

Show why offset pagination repeats or skips rows when data changes between requests, and how a keyset cursor avoids it.

## Run it

```
cd rest-pagination
java Main.java
```

Expected output:

```
offset duplicates=1 cursor duplicates=0
OK
```

## What it proves

- With `offsetPage(5, 5)` after a new order is inserted at the front, the first row of page 1's tail shifts into page 2: exactly 1 duplicate.
- `cursorPage` filters on `id < cursor` (the equivalent of `WHERE id < :cursor ORDER BY id DESC LIMIT n`), so the same scenario gives 0 duplicates.
- The cursor is an opaque URL-safe base64 string of the last id seen (`next` in `Main.java`); clients never build it themselves.

## Trade-offs

- Cursors cannot jump to "page 7"; they only move forward from a known position.
- The cursor sorts on a single unique column. Sorting by a non-unique field needs a compound cursor.
- The demo sorts the whole list in memory; a real database needs an index on the sort column for the keyset query to be cheap.

## When not to use it

- Small, static lists, where offset pagination is simpler and nothing shifts underneath the reader.
- UIs that need random page access with a total page count.
