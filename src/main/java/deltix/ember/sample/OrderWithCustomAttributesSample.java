package deltix.ember.sample;

import deltix.anvil.util.codec.AlphanumericCodec;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.efix.message.field.PartyRole;
import deltix.efix.message.field.Tag;
import deltix.ember.message.trade.MutableOrderNewRequest;
import deltix.ember.message.trade.OrderType;
import deltix.ember.message.trade.Side;
import deltix.ember.message.trade.TimeInForce;
import deltix.ember.util.CustomAttributeListBuilder;
import deltix.qsrv.hf.pub.ExchangeCodec;

import javax.annotation.Nonnull;

/**
 * Same as OrderSubmitSample, but request includes custom order attributes
 */
public class OrderWithCustomAttributesSample extends SampleSupportTools {
    public static void main(String[] args) throws InterruptedException {
        sendRequest(
                (publication) -> {
                    Side side;
                    if (args.length == 0 || args[0].equals("BUY")) {
                        side = Side.BUY;
                    } else {
                        side = Side.SELL;
                    }

                    int size = (args.length > 1) ? Integer.parseInt(args[1]) : 100;
                    String symbol = (args.length > 2) ? args[2] : "SC03051216485710261";
                    double price = (args.length > 3) ? Double.parseDouble(args[3]) : Double.NaN;

                    MutableOrderNewRequest request = createNewOrderRequest(side, size, symbol, price);
                    publication.onNewOrderRequest(request);
                    System.out.println("New order request was sent " + request.getSourceId() + ':' + request.getOrderId());

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
        request.setDestinationId(AlphanumericCodec.encode("SIM"));
        request.setExchangeId(AlphanumericCodec.encode("FILL"));
        request.setAccount("GOLD");
        request.setSourceId(CLIENT_SOURCE_ID); // Identify order source
        request.setTimestamp(System.currentTimeMillis());

        addCustomAttributes(request);


        return request;
    }

    // Reusable helper
    private static CustomAttributeListBuilder attributeListBuilder = new CustomAttributeListBuilder();

    private static void addCustomAttributes(MutableOrderNewRequest request) {
        attributeListBuilder.clear();

        // Repeated group
        attributeListBuilder
                .addInteger(Tag.NoPartyIDs, 2)
                .addText(Tag.PartyID, "AAA")
                .addInteger(Tag.PartyRole, PartyRole.CLIENT_ID)
                .addText(Tag.PartyID, "BBB")
                .addInteger(Tag.PartyRole, PartyRole.AGENT);

        attributeListBuilder
                .addTimestamp(7050, System.currentTimeMillis() + 1000);

        request.setAttributes(attributeListBuilder.build());
    }
}
