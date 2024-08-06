package deltix.ember.sample;

import com.epam.deltix.dfp.Decimal64Utils;
import deltix.anvil.util.codec.AlphanumericCodec;
import deltix.ember.message.trade.MutableOrderNewRequest;
import deltix.ember.message.trade.OrderType;
import deltix.ember.message.trade.Side;
import deltix.ember.message.trade.TimeInForce;
import deltix.ember.message.trade.oms.MutablePositionResetRequest;

import javax.annotation.Nonnull;

/**
 * Sample that illustrates how to reset all positions for specified projection
 */
public class PositionResetSample extends SampleSupportTools {
    public static void main(String[] args) throws InterruptedException {
        sendRequest(
                (publication) -> {
                    MutablePositionResetRequest request = new MutablePositionResetRequest();
                    request.setProjection("Account/Symbol");
                    request.setDaily(false);
                    publication.onPositionReset(request);

                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @Nonnull
    private static MutableOrderNewRequest createNewOrderRequest(Side side, int size, String symbol, double price) {
        MutableOrderNewRequest request = new MutableOrderNewRequest();
        request.setOrderId(Long.toString(System.currentTimeMillis() % 100000000000L));
        request.setSide(side);
        request.setQuantity(Decimal64Utils.fromLong((long) size));
        request.setSymbol(symbol);

        if (!Double.isNaN(price))
            request.setLimitPrice(Decimal64Utils.fromDouble(price));
        request.setTimeInForce(request.hasLimitPrice() ? TimeInForce.DAY : TimeInForce.IMMEDIATE_OR_CANCEL);
        request.setDisplayQuantity(Decimal64Utils.fromLong((long) (size / 10)));
        request.setOrderType(request.hasLimitPrice() ? OrderType.LIMIT : OrderType.MARKET);
        request.setDestinationId(AlphanumericCodec.encode("SIMULATOR"));
        //request.setExchangeId(ExchangeCodec.codeToLong("FILL"));
        request.setAccount("GOLD");
        request.setSourceId(CLIENT_SOURCE_ID); // Identify order source
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }
}
