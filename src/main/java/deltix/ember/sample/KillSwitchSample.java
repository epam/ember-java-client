package deltix.ember.sample;


import com.typesafe.config.Config;
import deltix.ember.app.EmberConfig;
import deltix.ember.bus.MessageBusFactory;
import deltix.ember.bus.client.api.MessageBus;
import deltix.ember.bus.client.api.Publication;
import deltix.ember.bus.client.api.PublicationException;
import deltix.ember.message.risk.KillSwitchAction;
import deltix.ember.message.risk.MutableKillSwitchRequest;

/** Sample that illustrates RPC call to Halt trading on server */
public class KillSwitchSample {

    /**
     * Command line argument is projection path at which we want to halt trading. By default (nothing provided)
     * trading will be halted system-wide. Supplied projection path may localize kill switch to specific trading
     * projection. For example: Source[FIXTRADER1]/Account[GOLD]. See ES Risk documentation for more info.
     */
    public static void main (String [] args) {
        Config config = EmberConfig.load();
        try (MessageBus bus = MessageBusFactory.create(config)) {
            Publication publication = bus.addPublication();

            MutableKillSwitchRequest kill = new MutableKillSwitchRequest();
            kill.setAction(KillSwitchAction.HALT);
            kill.setProjectionPath(args.length > 0 ? args[0] : null);
            kill.setReason("Trading halted by Kill Switch Sample");

            try {
                publication.onKillSwitchRequest(kill);
                System.out.println("Kill switch request has been sent");
            } catch (PublicationException e) {
                System.out.println("Kill switch request was not sent due to: " + e.getMessage());
            }
        }
    }
}
