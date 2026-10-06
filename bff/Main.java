import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** BFF: one mobile-shaped endpoint fans out to two downstream services in parallel and trims the result. */
public class Main {
    static HttpServer serve(String path, String body) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext(path, ex -> reply(ex, body));
        s.start();
        return s;
    }

    static void reply(com.sun.net.httpserver.HttpExchange ex, String body) throws java.io.IOException {
        byte[] b = body.getBytes();
        ex.sendResponseHeaders(200, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }

    public static void main(String[] a) throws Exception {
        HttpServer orders = serve("/orders/7", "id=7;customerId=3;total=42.50;internalNotes=secret");
        HttpServer customers = serve("/customers/3", "id=3;name=Ada;email=ada@example.com;segment=gold");
        HttpClient c = HttpClient.newHttpClient();
        HttpServer bff = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        bff.createContext("/mobile/order-summary", ex -> {
            CompletableFuture<Map<String, String>> o = get(c, orders, "/orders/7");
            CompletableFuture<Map<String, String>> cu = get(c, customers, "/customers/3");
            // only what the mobile screen needs: no email, no internal notes
            String json = o.thenCombine(cu, (ord, cus) -> "{\"orderId\":" + ord.get("id")
                    + ",\"total\":" + ord.get("total") + ",\"customer\":\"" + cus.get("name") + "\"}").join();
            reply(ex, json);
        });
        bff.start();
        String out = c.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + bff.getAddress().getPort()
                + "/mobile/order-summary")).build(), HttpResponse.BodyHandlers.ofString()).body();
        orders.stop(0);
        customers.stop(0);
        bff.stop(0);
        System.out.println(out);
        if (!out.equals("{\"orderId\":7,\"total\":42.50,\"customer\":\"Ada\"}") || out.contains("secret"))
            throw new AssertionError("unexpected result");
        System.out.println("OK");
    }

    static CompletableFuture<Map<String, String>> get(HttpClient c, HttpServer s, String path) {
        return c.sendAsync(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + s.getAddress().getPort() + path)).build(),
                HttpResponse.BodyHandlers.ofString()).thenApply(r -> {
            Map<String, String> m = new HashMap<>();
            for (String kv : r.body().split(";")) {
                String[] p = kv.split("=", 2);
                m.put(p[0], p[1]);
            }
            return m;
        });
    }
}
