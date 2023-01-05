package deltix.ember.sample;

import com.typesafe.config.Config;
import deltix.anvil.util.annotation.Alphanumeric;
import deltix.anvil.util.codec.AlphanumericCodec;
import deltix.ember.app.EmberConfig;
import deltix.ember.bus.MessageBusFactory;
import deltix.ember.bus.client.api.MessageBus;
import deltix.ember.bus.client.api.Publication;
import deltix.ember.bus.client.api.PublicationException;
import deltix.ember.message.service.MutableServiceKillSwitchRequest;

public class ServiceKillSwitchSample {


    /**
     * Command line argument is service or algorithm ID  which we want to kill trading.
     */
    public static void main (String [] args) {

        String destination = (args.length > 0) ? args[0] : "SIMULATOR";
        @Alphanumeric long destinationId = AlphanumericCodec.encode(destination);

        Config config = EmberConfig.load();
        try (MessageBus bus = MessageBusFactory.create(config)) {
            Publication publication = bus.addPublication();

            MutableServiceKillSwitchRequest kill = new MutableServiceKillSwitchRequest();
            kill.setEnable(false);
            kill.setReason("Killed by sample");
            kill.setDestinationId(destinationId);

            try {
                publication.onServiceKillSwitchRequest(kill);
                System.out.println("Service Kill switch request has been sent");
            } catch (PublicationException e) {
                System.out.println("Service Kill switch request was not sent due to: " + e.getMessage());
            }
        }
    }
}
