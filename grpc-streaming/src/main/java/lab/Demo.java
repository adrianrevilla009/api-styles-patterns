package lab;

import io.grpc.*;
import io.grpc.stub.StreamObserver;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/** In-process gRPC: a unary call with a deadline that expires, and a server stream consumed to the end. */
public class Demo {
    static class Impl extends OrdersGrpc.OrdersImplBase {
        @Override
        public void get(OrderRequest r, StreamObserver<Order> out) {
            try { Thread.sleep(r.getId() == 99 ? 500 : 0); } catch (InterruptedException e) { return; } // 99 = slow order
            out.onNext(Order.newBuilder().setId(r.getId()).setStatus("SHIPPED").build());
            out.onCompleted();
        }

        @Override
        public void watch(WatchRequest r, StreamObserver<Order> out) {
            for (int i = 1; i <= r.getCount(); i++) out.onNext(Order.newBuilder().setId(i).setStatus("PAID").build());
            out.onCompleted();
        }
    }

    public static void main(String[] a) throws Exception {
        Server server = InProcessServerBuilder.forName("orders").directExecutor().addService(new Impl()).build().start();
        ManagedChannel ch = InProcessChannelBuilder.forName("orders").directExecutor().build();
        OrdersGrpc.OrdersBlockingStub stub = OrdersGrpc.newBlockingStub(ch);

        Order ok = stub.withDeadlineAfter(1, TimeUnit.SECONDS).get(OrderRequest.newBuilder().setId(7).build());
        String code;
        try {
            stub.withDeadlineAfter(50, TimeUnit.MILLISECONDS).get(OrderRequest.newBuilder().setId(99).build());
            code = "NONE";
        } catch (StatusRuntimeException e) { code = e.getStatus().getCode().name(); }

        int streamed = 0;
        for (Iterator<Order> it = stub.watch(WatchRequest.newBuilder().setCount(3).build()); it.hasNext(); it.next()) streamed++;

        ch.shutdownNow();
        server.shutdownNow();
        System.out.println("unary=" + ok.getStatus() + " deadline=" + code + " streamed=" + streamed);
        if (!code.equals("DEADLINE_EXCEEDED") || streamed != 3) throw new AssertionError("unexpected result");
        System.out.println("OK");
    }
}
