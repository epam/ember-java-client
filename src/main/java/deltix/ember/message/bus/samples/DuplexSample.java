package deltix.ember.message.bus.samples;

import deltix.anvil.util.ShutdownSignal;
import deltix.anvil.util.concurrent.strategy.BackoffIdleStrategy;
import deltix.anvil.util.concurrent.strategy.IdleStrategy;
import deltix.ember.bus.client.api.*;
import deltix.ember.bus.client.ebit.EbitMessageBus;

/**
 * Two-way message flow between server and client.
 */
public class DuplexSample {

    public static void main(final String[] args) {
        // This sample assumes that Ember RPC API uses TCP mode (ebit) and is running locally on port 8989
        try (final MessageBus client = new EbitMessageBus("localhost", 8989);
             final Duplex duplex = client.addDuplex()) {

            final Sender sender = new Sender(duplex);
            final Receiver receiver = new Receiver(duplex, sender);

            final ShutdownSignal exit = new ShutdownSignal();
            final IdleStrategy idle = new BackoffIdleStrategy();

            do {
                int work = 0;

                work += sender.send();
                work += receiver.receive();

                idle.idle(work);
            } while (!exit.isSignaled());
        }
    }

}
