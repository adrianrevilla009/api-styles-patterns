import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.util.*;

/** Server-Sent Events: the server pushes order status events over one long-lived HTTP response. */
public class Main {
    public static void main(String[] a) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/orders/events", ex -> {
            ex.getResponseHeaders().add("Content-Type", "text/event-stream");
            ex.getResponseHeaders().add("Cache-Control", "no-cache");
            ex.sendResponseHeaders(200, 0);
            try (OutputStream o = ex.getResponseBody()) {
                for (String st : List.of("CREATED", "PAID", "SHIPPED")) {
                    o.write(("event: status\ndata: " + st + "\n\n").getBytes());
                    o.flush();
                    Thread.sleep(50);
                }
            } catch (InterruptedException ignored) { }
        });
        s.start();
        HttpResponse<java.util.stream.Stream<String>> r = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + s.getAddress().getPort() + "/orders/events")).build(),
                HttpResponse.BodyHandlers.ofLines());
        List<String> got = r.body().filter(l -> l.startsWith("data: ")).map(l -> l.substring(6)).toList();
        s.stop(0);
        System.out.println("content-type=" + r.headers().firstValue("content-type").orElse("") + " events=" + got);
        if (!got.equals(List.of("CREATED", "PAID", "SHIPPED"))) throw new AssertionError("unexpected events");
        System.out.println("OK");
    }
}
