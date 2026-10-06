import java.util.*;
import java.util.stream.*;

/** Offset vs cursor pagination over an in-memory "orders" table, with a concurrent insert. */
public class Main {
    record Order(int id, String item) {}

    static final List<Order> TABLE = new ArrayList<>();

    static List<Order> offsetPage(int offset, int limit) {
        return TABLE.stream().skip(offset).limit(limit).collect(Collectors.toList());
    }

    /** Keyset: "WHERE id < :cursor ORDER BY id DESC LIMIT :limit"; the cursor is opaque base64. */
    static List<Order> cursorPage(String cursor, int limit) {
        int after = cursor == null ? Integer.MAX_VALUE
                : Integer.parseInt(new String(Base64.getUrlDecoder().decode(cursor)));
        return TABLE.stream().sorted(Comparator.comparingInt(Order::id).reversed())
                .filter(o -> o.id() < after).limit(limit).collect(Collectors.toList());
    }

    static String next(List<Order> page) {
        return Base64.getUrlEncoder().encodeToString(
                String.valueOf(page.get(page.size() - 1).id()).getBytes());
    }

    static void reset() {
        TABLE.clear();
        for (int i = 1; i <= 10; i++) TABLE.add(0, new Order(i, "item" + i)); // newest first
    }

    static void insertNewest() { TABLE.add(0, new Order(TABLE.size() + 1, "new")); }

    static int duplicates(List<Order> one, List<Order> two) {
        Set<Integer> seen = new HashSet<>();
        int dup = 0;
        for (Order o : Stream.concat(one.stream(), two.stream()).toList()) if (!seen.add(o.id())) dup++;
        return dup;
    }

    public static void main(String[] a) {
        // Offset: read page 1, a new order arrives, read page 2 -> one row repeated.
        reset();
        List<Order> p1 = offsetPage(0, 5);
        insertNewest();
        int dupOffset = duplicates(p1, offsetPage(5, 5));
        // Cursor: same scenario, the cursor anchors on the last id seen.
        reset();
        List<Order> c1 = cursorPage(null, 5);
        insertNewest();
        int dupCursor = duplicates(c1, cursorPage(next(c1), 5));
        System.out.println("offset duplicates=" + dupOffset + " cursor duplicates=" + dupCursor);
        if (dupOffset != 1 || dupCursor != 0) throw new AssertionError("unexpected result");
        System.out.println("OK");
    }
}
