package deltix.ember.message.bus.samples;

import deltix.anvil.util.codec.AlphanumericCodec;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.ember.bus.client.api.Publication;
import deltix.ember.bus.client.api.PublicationException;
import deltix.ember.message.common.ApiMessage;
import deltix.ember.message.trade.*;
import deltix.gflog.Log;
import deltix.gflog.LogFactory;

import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;


/** This sample component periodically sends dummy orders to Ember Java API Gateway */
final class Sender implements Receiver.Handler {

    private static final Log LOG = LogFactory.getLog(Sender.class);

    private static final String SYMBOL = "BTCUSD";

    private static final long SOURCE_ID = AlphanumericCodec.encode("SAMPLE");
    private static final long DESTINATION_ID = AlphanumericCodec.encode("SIM");
    private static final long EXCHANGE_ID = AlphanumericCodec.encode("FILL");

    private static final long QUANTITY = Decimal64Utils.parse("1");
    private static final long PRICE = Decimal64Utils.parse("34567.89");

    private final HashSet<String> activeOrders = new HashSet<>();
    private final Publication publication;

    private long orderId = ThreadLocalRandom.current().nextLong(0, 1000000000);
    private long timeOfNextSend = Long.MIN_VALUE;

    public Sender(final Publication publication) {
        this.publication = publication;
    }

    public int send() {
        final long now = System.currentTimeMillis();

        if (now >= timeOfNextSend) {
            timeOfNextSend = now + TimeUnit.SECONDS.toMillis(5);
            final String orderId = "ID:" + this.orderId++;

            if (sendOrderNewRequest(orderId)) {
                activeOrders.add(orderId);
            }

            return 1; // how much work units were done (hint for idle strategy)
        }

        return 0;
    }


    @Override
    public void onMessage(final ApiMessage message) {
        if (message instanceof OrderEvent) {
            final OrderEvent event = (OrderEvent) message;

            // Here we perform simplified order state tracking (keeping list of currently active orders)
            if (isFinalOrder(event)) {
                final String orderId = event.getOrderId().toString();
                activeOrders.remove(orderId);
            }
        }
    }

    @Override
    public void onDisconnected() {
        LOG.warn("Lost connection to Ember Gateway");
    }

    @Override
    public void onConnected() {
        LOG.warn("Restored connection to Ember Gateway");

        // Recovery procedure: fetch most recent state of active orders (some may no longer be active)
        for (final String orderId : activeOrders) {
            sendOrderStatusRequest(orderId); // will result in OrderStatusEvent
        }
    }


    private boolean sendOrderNewRequest(final String orderId) {
        final MutableOrderNewRequest request = new MutableOrderNewRequest();

        request.setOrderId(orderId);
        request.setSourceId(SOURCE_ID);
        request.setDestinationId(DESTINATION_ID);
        request.setExchangeId(EXCHANGE_ID);
        request.setSymbol(SYMBOL);
        request.setSide(Side.BUY);
        request.setTimeInForce(TimeInForce.DAY);
        request.setOrderType(OrderType.LIMIT);
        request.setLimitPrice(PRICE);
        request.setQuantity(QUANTITY);
        request.setTimestamp(System.currentTimeMillis());

        try {
            publication.onNewOrderRequest(request);
            LOG.info("Sent:          %s").with(request);
            return true;
        } catch (final PublicationException e) {
            LOG.warn("Didn't send:   %s").with(e.getMessage());
            return false;
        }
    }

    /** Request the latest state of a given order */
    private boolean sendOrderStatusRequest(final String orderId) {
        final MutableOrderStatusRequest request = new MutableOrderStatusRequest();

        request.setSourceId(SOURCE_ID);
        request.setOrderId(orderId);

        try {
            publication.onOrderStatusRequest(request);
            LOG.info("Sent:          %s").with(request);
            return true;
        } catch (final PublicationException e) {
            LOG.warn("Didn't send:   %s").with(e.getMessage());
            return false;
        }
    }

    private static boolean isFinalOrder(final OrderEvent event) {
        final OrderStatus status = event.getOrderStatus();
        return status == OrderStatus.COMPLETELY_FILLED || status == OrderStatus.CANCELED || status == OrderStatus.REJECTED;
    }

}
