import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Signed webhook: HMAC-SHA256 over "timestamp.body", constant-time verify, sender retries with backoff. */
public class Main {
    static final String SECRET = "demo-secret-not-real"; // shared secret; in real life it comes from env

    static String sign(String ts, String body) throws Exception {
        Mac m = Mac.getInstance("HmacSHA256");
        m.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(m.doFinal((ts + "." + body).getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] a) throws Exception {
        AtomicInteger valid = new AtomicInteger();
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/hook", ex -> {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            String ts = ex.getRequestHeaders().getFirst("X-Timestamp");
            String sig = ex.getRequestHeaders().getFirst("X-Signature");
            int code;
            try {
                boolean fresh = Math.abs(System.currentTimeMillis() / 1000 - Long.parseLong(ts)) < 300; // replay window
                boolean ok = MessageDigest.isEqual(sign(ts, body).getBytes(), String.valueOf(sig).getBytes());
                code = !(fresh && ok) ? 401 : valid.incrementAndGet() < 3 ? 503 : 200; // fail twice, then accept
            } catch (Exception e) { code = 401; }
            ex.sendResponseHeaders(code, -1);
            ex.close();
        });
        s.start();
        String url = "http://127.0.0.1:" + s.getAddress().getPort() + "/hook";
        HttpClient c = HttpClient.newHttpClient();

        int forged = send(c, url, "{\"orderId\":1}", "deadbeef");
        int status = 0, attempts = 0;
        String body = "{\"orderId\":1,\"event\":\"order.paid\"}";
        for (long backoff = 20; attempts < 5 && status != 200; backoff *= 2) {
            attempts++;
            status = send(c, url, body, null);
            if (status != 200) Thread.sleep(backoff);
        }
        s.stop(0);
        System.out.println("forged=" + forged + " final=" + status + " attempts=" + attempts);
        if (forged != 401 || status != 200 || attempts != 3) throw new AssertionError("unexpected result");
        System.out.println("OK");
    }

    static int send(HttpClient c, String url, String body, String forcedSig) throws Exception {
        String ts = String.valueOf(System.currentTimeMillis() / 1000);
        HttpRequest r = HttpRequest.newBuilder(URI.create(url)).header("X-Timestamp", ts)
                .header("X-Signature", forcedSig != null ? forcedSig : sign(ts, body))
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return c.send(r, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
