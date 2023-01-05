package deltix.ember.message.bus.samples;

import deltix.anvil.util.ShutdownSignal;
import deltix.anvil.util.concurrent.strategy.BackoffIdleStrategy;
import deltix.anvil.util.concurrent.strategy.IdleStrategy;
import deltix.ember.bus.client.api.MessageBus;
import deltix.ember.bus.client.api.Publication;
import deltix.ember.bus.client.ebit.EbitMessageBus;

/**
 * One-way message flow from client to server.
 */
public class PublicationSample {

    public static void main(final String[] args) {
        // This sample assumes that Ember RPC API uses TCP mode (ebit) and is running locally on port 8989
        try (final MessageBus client = new EbitMessageBus("localhost", 8989);
             final Publication publication = client.addPublication()) {

            final Sender sender = new Sender(publication);
            final ShutdownSignal exit = new ShutdownSignal();
            final IdleStrategy idle = new BackoffIdleStrategy();

            do {
                final int work = sender.send();
                idle.idle(work);
            } while (!exit.isSignaled());
        }
    }

}
