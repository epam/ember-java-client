package deltix.ember.message.bus.samples;

import deltix.ember.bus.client.api.MultiplexedSubscriber;
import deltix.ember.bus.client.api.Subscriber;
import deltix.ember.bus.client.api.Subscription;
import deltix.ember.message.common.ApiMessage;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

/** This component receives events from Ember Gateway */
final class Receiver {

    private static final Log LOG = LogFactory.getLog(Receiver.class);

    private static final int MESSAGE_LIMIT = 16;

    private final Subscription subscription;
    private final Subscriber subscriber;
    private final Handler handler;

    private long connectionId;

    public Receiver(final Subscription subscription) {
        this(subscription, null);
    }

    public Receiver(final Subscription subscription, final Handler handler) {
        this.subscription = subscription;
        this.subscriber = new MultiplexedSubscriber(this::onMessage);
        this.handler = handler;
        this.connectionId = this.subscription.getConnectionId();
    }

    public int receive() {
        final int work = subscription.poll(subscriber, MESSAGE_LIMIT);

        if (work <= 0) {
            checkConnection();
        }

        return work;
    }

    private void checkConnection() {
        final long previous = connectionId;
        final long next = subscription.getConnectionId();

        if (previous != next) {
            connectionId = next;

            if (previous >= 0) {
                onDisconnect();
            }

            if (next >= 0) {
                onConnect();
            }
        }
    }

    private void onConnect() {
        LOG.info("Reconnected");

        if (handler != null) {
            handler.onConnected();
        }
    }

    private void onDisconnect() {
        LOG.warn("Disconnected");

        if (handler != null) {
            handler.onDisconnected();
        }
    }

    private void onMessage(final ApiMessage message) {
        LOG.info("Received:      %s").with(message);

        if (handler != null) {
            handler.onMessage(message);
        }
    }


    interface Handler {

        void onConnected();

        void onDisconnected();

        void onMessage(ApiMessage message);

    }

}
