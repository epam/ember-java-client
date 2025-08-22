package deltix.ember.sample;

import com.typesafe.config.Config;
import deltix.anvil.util.codec.AlphanumericCodec;
import deltix.ember.app.EmberConfig;
import deltix.ember.bus.MessageBusFactory;
import deltix.ember.bus.client.api.*;
import deltix.ember.message.common.ApiMessage;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;

import java.util.function.Consumer;

public class SampleSupportTools {
    protected static final long CLIENT_SOURCE_ID = AlphanumericCodec.encode("SAMPLE");

    protected static final Log LOGGER = LogFactory.getLog("Samples");

    protected static void sendRequest(Consumer<Publication> callback) throws InterruptedException {
        sendRequest(callback, EmberConfig.load(true, true), System.out::println);
    }

    protected static void sendRequest(Consumer<Publication> callback, Consumer<ApiMessage> eventListener) throws InterruptedException {
        sendRequest(callback, EmberConfig.load(true, true), eventListener);
    }

    protected static void sendRequest(Consumer<Publication> callback, Config config, Consumer<ApiMessage> eventListener) throws InterruptedException {
        try (MessageBus bus = MessageBusFactory.create(config)) {

            // Prepare event listener prior to request submission
            Duplex duplex = bus.addDuplex();
            Thread consumerThread = new Thread(() -> {
                MultiplexedSubscriber subscriber = new MultiplexedSubscriber(eventListener);
                while ( ! Thread.currentThread().isInterrupted()) {
                    duplex.poll(subscriber, 128);
                }
            }, "Event Processor Thread");
            consumerThread.start();


            try {
                callback.accept(duplex);
            } catch (PublicationException e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace();
            }

            consumerThread.interrupt();
            consumerThread.join();
            duplex.close();
        }
    }

}
