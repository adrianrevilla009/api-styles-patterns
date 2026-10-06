# soap-jaxws-legacy

`Main.java` sends a hand-built SOAP 1.1 `getOrder` request to a JDK `HttpServer` that parses it and answers with a SOAP response.

## Goal

Show what a SOAP call is on the wire: an HTTP POST of an XML envelope with a `SOAPAction` header. JAX-WS itself is not used because it left the JDK in Java 11.

## Run it

```
cd soap-jaxws-legacy
java Main.java
```

Expected output (second line shortened to the response body):

```
<o:getOrderResponse xmlns:o="http://example.com/orders">...<o:status>SHIPPED</o:status><o:orderId>7</o:orderId></o:getOrderResponse></soap:Body></soap:Envelope>
OK
```

## What it proves

- The request is `Content-Type: text/xml` with `SOAPAction: "getOrder"` and an envelope using the `http://schemas.xmlsoap.org/soap/envelope/` namespace.
- The server reads `orderId` by namespace with the JDK XML parser and returns `SHIPPED` for order 7.
- DOCTYPE declarations are disabled on the parser (`disallow-doctype-decl`), which blocks XXE attacks.

## Trade-offs

- There is no WSDL, schema validation, SOAP fault handling or WS-Security; the contract is only implied by the code.
- Envelopes are strings built by hand, which is fine for a demo and fragile for anything larger.
- Real legacy integration would use generated clients (for example Apache CXF) from the service WSDL.

## When not to use it

- For new APIs: prefer REST, gRPC or GraphQL.
- Use it only to read or test an existing SOAP service you must integrate with.
