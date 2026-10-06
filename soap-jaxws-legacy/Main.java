import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * SOAP awareness: a SOAP 1.1 call is an HTTP POST of an XML envelope with a SOAPAction header.
 * JAX-WS left the JDK in Java 11, so this lab builds the envelope by hand with the JDK XML parser.
 */
public class Main {
    static final String NS = "http://example.com/orders";
    static final String ENV = "http://schemas.xmlsoap.org/soap/envelope/";

    public static void main(String[] a) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/orders", ex -> {
            try {
                var f = DocumentBuilderFactory.newInstance();
                f.setNamespaceAware(true);
                f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); // no XXE
                var doc = f.newDocumentBuilder().parse(new ByteArrayInputStream(ex.getRequestBody().readAllBytes()));
                String id = doc.getElementsByTagNameNS(NS, "orderId").item(0).getTextContent();
                byte[] b = ("<?xml version=\"1.0\"?><soap:Envelope xmlns:soap=\"" + ENV + "\"><soap:Body>"
                        + "<o:getOrderResponse xmlns:o=\"" + NS + "\"><o:status>SHIPPED</o:status>"
                        + "<o:orderId>" + id + "</o:orderId></o:getOrderResponse></soap:Body></soap:Envelope>").getBytes();
                ex.getResponseHeaders().add("Content-Type", "text/xml; charset=utf-8");
                ex.sendResponseHeaders(200, b.length);
                ex.getResponseBody().write(b);
            } catch (Exception e) { ex.sendResponseHeaders(500, -1); }
            ex.close();
        });
        s.start();
        String req = "<soap:Envelope xmlns:soap=\"" + ENV + "\"><soap:Body><o:getOrder xmlns:o=\"" + NS
                + "\"><o:orderId>7</o:orderId></o:getOrder></soap:Body></soap:Envelope>";
        String res = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                        URI.create("http://127.0.0.1:" + s.getAddress().getPort() + "/orders"))
                .header("Content-Type", "text/xml; charset=utf-8").header("SOAPAction", "\"getOrder\"")
                .POST(HttpRequest.BodyPublishers.ofString(req)).build(), HttpResponse.BodyHandlers.ofString()).body();
        s.stop(0);
        System.out.println(res.substring(res.indexOf("<o:getOrderResponse")));
        if (!res.contains("<o:status>SHIPPED</o:status>") || !res.contains("<o:orderId>7</o:orderId>"))
            throw new AssertionError("unexpected result");
        System.out.println("OK");
    }
}
