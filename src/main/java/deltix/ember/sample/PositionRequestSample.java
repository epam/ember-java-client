package deltix.ember.sample;

import com.epam.deltix.dfp.Decimal;
import com.epam.deltix.dfp.Decimal64Utils;
import deltix.anvil.util.ShutdownSignal;
import deltix.anvil.util.codec.AlphanumericCodec;
import deltix.ember.message.trade.oms.MutablePositionRequest;
import deltix.ember.message.trade.oms.PositionReport;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import deltix.util.io.CSVWriter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** This sample fetches all positions for Exchange/Symbol projection and saves them into specified CSV file */
public class PositionRequestSample extends SampleSupportTools {

    // Here we show how to fetch per-exchange instrument positions:
    // (Make sure server is configured with engine.allowUnrestrictedPositionRequests=true setting
    //   if you plan to fetch server wide positions via Ember client API)
    private static final String positionProjection = "Exchange/Symbol";

    private static final Log LOGGER = LogFactory.getLog(PositionRequestSample.class);

    public static void main(String[] args) throws InterruptedException, IOException {
        if (args.length != 1)
            throw new IllegalArgumentException("Expecting output CSV filename as argument");
        try (CSVWriter csvWriter = createCSV(args[0])) {
            final String positionRequestId = "PR#" + System.currentTimeMillis(); // generate unique request ID
            final ShutdownSignal shutdownSignal = new ShutdownSignal();

            sendRequest(
                    (publication) -> {
                        MutablePositionRequest request = createPositionRequest(positionRequestId, positionProjection);
                        publication.onPositionRequest(request);
                        LOGGER.info("Sent position request %s for projection %s").with(request.getRequestId()).with(request.getProjection());

                        if (shutdownSignal.await(5, TimeUnit.MINUTES))
                            LOGGER.info("Success");
                        else
                            LOGGER.error("Timeout waiting for position response!");

                    },

                    (message) -> {
                        if (message instanceof PositionReport) {
                            PositionReport report = (PositionReport) message;
                            LOGGER.info("Received position report %s").with(report);

                            if (report.getRequestId().equals(positionRequestId)) {
                                if (!report.isFound()) {
                                    LOGGER.error("Projection %s was not found").with(positionProjection);
                                } else {
                                    writeReport(report, csvWriter);
                                }
                                if (report.isLast())
                                    shutdownSignal.signal();
                            }
                        }
                    }
            );

        }

    }

    @Nonnull
    private static MutablePositionRequest createPositionRequest(String requestId, String projection) {
        MutablePositionRequest request = new MutablePositionRequest();
        request.setRequestId(requestId);
        request.setTimestamp(System.currentTimeMillis());
        request.setProjection(projection); // note: projection must be one of supported by Ember (configured in Ember risk)
        // Optional filters:
        //request.setSourceId();
        //request.setExchangeId();
        //request.setAccount();
        //request.setModuleKey();
        //request.setSymbol();
        //...
        return request;
    }

    private static CSVWriter createCSV(String fileName) throws IOException {
        CSVWriter writer = new CSVWriter(new File(fileName));
        writer.writeLine("Symbol", "Exchange", "Size", "Open BUY Qty", "Open SELL Qty", "Average Cost", "Realized P&L", "Unrealized P&L", "Market Value");
        return writer;
    }

    private static void writeReport(PositionReport report, CSVWriter writer) {
        try {
            writer.writeLine(report.getSymbol(),
                    AlphanumericCodec.decode(report.getExchangeId()),
                    format(report.getSize()),
                    format(report.getOpenBuySize()),
                    format(report.getOpenSellSize()),
                    format(report.getAverageCost()),
                    format(report.getRealizedPnL()),
                    format(report.getUnrealizedPnL()),
                    format(report.getMarketValue()));
        } catch (IOException ex) {
            LOGGER.error("Failed to write to CSV file: %s").with(ex.getMessage());
        }
    }

    private static String format(@Decimal long number) {
        if (Decimal64Utils.isNaN(number))
            return "";
        return Decimal64Utils.toString(number);
    }
}
