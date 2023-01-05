package deltix.ember.sample;

import deltix.anvil.util.codec.AlphanumericCodec;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.ember.message.trade.*;

import javax.annotation.Nonnull;

/**
 * Created by Andy on 11/2/2017.
 */
public class OrderReplaceSample extends SampleSupportTools {
    public static void main(String[] args) throws InterruptedException {
        sendRequest(
            (publication) -> {
                MutableOrderNewRequest submitRequest = createNewOrderRequest(Side.BUY, 100, "EUR/USD", 25.9);
                publication.onNewOrderRequest(submitRequest);
                System.out.println("New order request was sent " + submitRequest.getSourceId() + ':' + submitRequest.getOrderId());

                MutableOrderReplaceRequest replaceRequest = createReplaceOrderRequest(submitRequest.getOrderId(), Side.BUY, 200, "EUR/USD", 25.9);
                publication.onReplaceOrderRequest(replaceRequest);
                System.out.println("Replace request was sent " + replaceRequest.getSourceId() + ':' + replaceRequest.getOrderId());
            }
        );
    }

    @Nonnull
    private static MutableOrderNewRequest createNewOrderRequest(Side side, int size, String symbol, double price) {
        MutableOrderNewRequest request = new MutableOrderNewRequest();
        request.setSourceId(CLIENT_SOURCE_ID); // Identify order source
        request.setOrderId("#" + System.currentTimeMillis()); // naive way to get unique order ID
        request.setSide(side);
        request.setQuantity(Decimal64Utils.fromLong((long) size));
        request.setSymbol(symbol);
        request.setLimitPrice(Decimal64Utils.fromDouble(price));
        request.setTimeInForce(TimeInForce.DAY);
        request.setDisplayQuantity(Decimal64Utils.fromLong((long) (size / 10)));
        request.setOrderType(OrderType.CUSTOM); // algo order
        request.setDestinationId(AlphanumericCodec.encode("SIM"));
        request.setExchangeId(AlphanumericCodec.encode("FILL"));
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    @Nonnull
    private static MutableOrderReplaceRequest createReplaceOrderRequest (CharSequence originalOrderId, Side side, int size, String symbol, double price) {
        MutableOrderReplaceRequest request = new MutableOrderReplaceRequest();
        request.setSourceId(CLIENT_SOURCE_ID); // Identify order source
        request.setOrderId("R#" + System.currentTimeMillis());
        request.setOriginalOrderId(originalOrderId);
        request.setSide(side);
        request.setQuantity(Decimal64Utils.fromLong((long) size));
        request.setSymbol(symbol);
        request.setOrderType(OrderType.LIMIT);
        request.setLimitPrice(Decimal64Utils.fromDouble(price));
        request.setTimeInForce(TimeInForce.GOOD_TILL_CANCEL);
        request.setDestinationId(AlphanumericCodec.encode("SIM"));
        request.setExchangeId(AlphanumericCodec.encode("FILL"));
        request.setTimestamp(System.currentTimeMillis()); // NB!
        return request;
    }
}
