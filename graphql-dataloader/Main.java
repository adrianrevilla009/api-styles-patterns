import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * N+1 and its DataLoader fix, without a GraphQL library: resolving { orders { customer { name } } }.
 * The loader collects keys requested during one "tick" and issues a single batched fetch.
 */
public class Main {
    static int queries = 0;

    static Map<Integer, String> fetchCustomers(Collection<Integer> ids) { // "SELECT ... WHERE id IN (...)"
        queries++;
        Map<Integer, String> m = new HashMap<>();
        ids.forEach(id -> m.put(id, "customer-" + id));
        return m;
    }

    static class DataLoader<K, V> {
        private final Function<Collection<K>, Map<K, V>> batch;
        private final Map<K, CompletableFuture<V>> pending = new LinkedHashMap<>();

        DataLoader(Function<Collection<K>, Map<K, V>> batch) { this.batch = batch; }

        CompletableFuture<V> load(K key) { return pending.computeIfAbsent(key, k -> new CompletableFuture<>()); }

        void dispatch() {
            Map<K, V> res = batch.apply(new ArrayList<>(pending.keySet()));
            pending.forEach((k, f) -> f.complete(res.get(k)));
            pending.clear();
        }
    }

    public static void main(String[] a) {
        int[] orderCustomer = {1, 2, 1, 3, 2, 4, 5, 1, 2, 3}; // 10 orders

        queries = 0; // naive: one query per order
        for (int c : orderCustomer) fetchCustomers(List.of(c));
        int naive = queries;

        queries = 0; // dataloader: resolvers enqueue keys, one dispatch, duplicates collapse
        DataLoader<Integer, String> loader = new DataLoader<>(Main::fetchCustomers);
        List<CompletableFuture<String>> out = new ArrayList<>();
        for (int c : orderCustomer) out.add(loader.load(c));
        loader.dispatch();
        out.forEach(CompletableFuture::join);
        int batched = queries;

        System.out.println("naive queries=" + naive + " dataloader queries=" + batched);
        if (naive != 10 || batched != 1) throw new AssertionError("unexpected result");
        System.out.println("OK");
    }
}
