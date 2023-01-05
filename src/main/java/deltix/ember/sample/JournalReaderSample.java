package deltix.ember.sample;

import com.typesafe.config.Config;
import deltix.ember.app.EmberAppHelper;
import deltix.ember.app.EmberConfig;
import deltix.ember.journal.reader.EmberBinaryJournalReader;

import java.util.function.Consumer;

public class JournalReaderSample {

    public static void main (String [] args) {
        final Config config = EmberConfig.load();
        final Consumer<Object> consumer = (message) -> System.out.println(message);

        try (final EmberBinaryJournalReader reader = EmberAppHelper.createObjectJournalReader(config)) {
            // read each message using provided consumer
            while (reader.read(consumer)) ;
        }

    }
}
