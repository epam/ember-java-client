package deltix.ember.sample;

import deltix.anvil.util.ShutdownSignal;
import deltix.ember.message.risk.MutableRiskTableSnapshotRequest;
import deltix.ember.message.risk.RiskCondition;
import deltix.ember.message.risk.RiskLimit;
import deltix.ember.message.risk.RiskTableSnapshotResponse;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import deltix.util.collections.generated.ObjectList;
import deltix.util.io.CSVWriter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** This sample fetches all risk table rows for a given projection and saves them into specified CSV file */
public class RiskTableRequestSample extends SampleSupportTools {

    private static final Log LOGGER = LogFactory.getLog(RiskTableRequestSample.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length != 2)
            throw new IllegalArgumentException("Expecting risk projection and output CSV filename as arguments");
        
        final String projection = args[0];
        final CSVWriter csvWriter = createCSV(args[1]);
        final String requestId = "RTR#" + System.currentTimeMillis(); // generate unique request ID
        final ShutdownSignal shutdownSignal = new ShutdownSignal();

        sendRequest(
                (publication) -> {
                    MutableRiskTableSnapshotRequest request = createRiskTableSnapshotRequest(requestId, projection);
                    publication.onRiskTableSnapshotRequest(request);
                    LOGGER.info("Sent risk table snapshot request %s for projection %s").with(request.getRequestId()).with(request.getProjection());

                    if (shutdownSignal.await(5, TimeUnit.MINUTES))
                        LOGGER.info("Success");
                    else
                        LOGGER.error("Timeout waiting for risk table snapshot response!");

                },

                (message) -> {
                    if (message instanceof RiskTableSnapshotResponse) {
                        RiskTableSnapshotResponse response = (RiskTableSnapshotResponse) message;
                        LOGGER.info("Received risk table snapshot response %s").with(response);

                        if (response.getRequestId().equals(requestId)) {
                            if (!response.isSuccess()) {
                                LOGGER.error("Request failed: %s").with(response.getErrorMessage());
                                shutdownSignal.signal();
                            } else {
                                if (response.hasConditions() || response.hasLimits()) {
                                    writeReport(response, csvWriter);
                                }
                                if (response.isLast())
                                    shutdownSignal.signal();
                            }
                        }
                    }
                }
        );

        csvWriter.close();

    }

    @Nonnull
    private static MutableRiskTableSnapshotRequest createRiskTableSnapshotRequest(String requestId, String projection) {
        MutableRiskTableSnapshotRequest request = new MutableRiskTableSnapshotRequest();
        request.setRequestId(requestId);
        request.setTimestamp(System.currentTimeMillis());
        request.setSourceId(CLIENT_SOURCE_ID);
        request.setProjection(projection); // note: projection must be one of supported by Ember (configured in Ember risk)
        return request;
    }

    private static CSVWriter createCSV(String fileName) throws IOException {
        CSVWriter writer = new CSVWriter(new File(fileName));
        // Header will be written dynamically based on first response
        return writer;
    }

    private static boolean headerWritten = false;

    private static void writeReport(RiskTableSnapshotResponse response, CSVWriter writer) {
        try {
            ObjectList<RiskCondition> conditions = response.getConditions();
            ObjectList<RiskLimit> limits = response.getLimits();

            if (!headerWritten) {
                // Write header row
                String[] header = new String[conditions.size() + limits.size()];
                int index = 0;
                for (int i = 0; i < conditions.size(); i++) {
                    RiskCondition condition = conditions.get(i);
                    header[index++] = condition.getProjectionKey().name();
                }
                for (int i = 0; i < limits.size(); i++) {
                    RiskLimit limit = limits.get(i);
                    header[index++] = limit.getName().toString();
                }
                writer.writeLine((Object[]) header);
                headerWritten = true;
            }

            // Write data row
            String[] row = new String[conditions.size() + limits.size()];
            int index = 0;
            for (int i = 0; i < conditions.size(); i++) {
                RiskCondition condition = conditions.get(i);
                CharSequence value = condition.getValue();
                row[index++] = (value != null) ? value.toString() : "";
            }
            for (int i = 0; i < limits.size(); i++) {
                RiskLimit limit = limits.get(i);
                CharSequence value = limit.getValue();
                row[index++] = (value != null) ? value.toString() : "";
            }
            writer.writeLine((Object[]) row);
        } catch (IOException ex) {
            LOGGER.error("Failed to write to CSV file: %s").with(ex.getMessage());
        }
    }
}

