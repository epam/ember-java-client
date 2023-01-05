package deltix.ember.sample;

import deltix.anvil.util.codec.AlphanumericCodec;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.ember.message.trade.*;

import javax.annotation.Nonnull;

/**
 * Sample that illustrates how to send new trade order and cancel it
 */
public class OrderCancelSample extends SampleSupportTools {
    public static void main(String[] args) throws InterruptedException {
        sendRequest(
            (publication) -> {
                MutableOrderNewRequest submitRequest = createNewOrderRequest(Side.BUY, 1000, "EUR/USD", 1.13);
                publication.onNewOrderRequest(submitRequest);
                System.out.println("New order request was sent " + submitRequest.getSourceId() + ':' + submitRequest.getOrderId());

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                MutableOrderCancelRequest cancelRequest = createCancelOrderRequest(submitRequest.getOrderId());
                publication.onCancelOrderRequest(cancelRequest);
                System.out.println("Cancel request was sent " + cancelRequest.getSourceId() + ':' + cancelRequest.getOrderId());
            }
        );
    }


    @Nonnull
    private static MutableOrderNewRequest createNewOrderRequest(Side side, int size, String symbol, double price) {
        MutableOrderNewRequest request = new MutableOrderNewRequest();
        request.setSourceId(CLIENT_SOURCE_ID); // Identify order source
        request.setOrderId(Long.toString(System.currentTimeMillis() % 100000000000L)); // naive way to get unique order ID
        request.setSide(side);
        request.setQuantity(Decimal64Utils.fromLong((long) size));
        request.setSymbol(symbol);
        request.setLimitPrice(Decimal64Utils.fromDouble(price));
        request.setTimeInForce(TimeInForce.DAY);
        request.setDisplayQuantity(Decimal64Utils.fromLong((long) (size / 10)));
        request.setOrderType(OrderType.LIMIT); // algo order
        request.setDestinationId(AlphanumericCodec.encode("MF"));
        request.setExchangeId(AlphanumericCodec.encode("HOTSPOT"));
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    @Nonnull
    private static MutableOrderCancelRequest createCancelOrderRequest(CharSequence orderId) {
        MutableOrderCancelRequest request = new MutableOrderCancelRequest();
        request.setSourceId(CLIENT_SOURCE_ID); // Identify order source
        request.setOrderId(orderId);
        request.setRequestId("#" + System.currentTimeMillis()); // naive way to get unique request ID
        request.setTimestamp(System.currentTimeMillis());
        request.setReason("Cancelled by sample code");
        return request;
    }

}
