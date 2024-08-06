package deltix.ember.sample;

import com.epam.deltix.dfp.Decimal64Utils;
import deltix.anvil.util.AsciiStringBuilder;
import deltix.ember.app.EmberAppInfo;
import deltix.ember.message.risk.ProjectionKey;
import deltix.ember.message.trade.oms.MutablePositionSnapshot;
import deltix.util.csvx.CSVXReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This sample shows how to load Positions for a specific projection from a CSV file.
 * CSV file header (the first line) must define projection to be used for loading.
 * The last column must be called "Size" (it will contain the size of each position to load.
 * Header example:
 * <pre>
 *     Trader,Destination,Currency,Size
 * </pre>
 *
 *
 */
public class PositionLoadFromCSV extends SampleSupportTools {

    public static void main(String[] args) throws InterruptedException {
        sendRequest(
            (publication) -> {

                try {
                    for (MutablePositionSnapshot request : loadPositionsFromCSV()) {
                        publication.onPositionSnapshot(request);
                        System.out.println("Sent position update request " + request);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        );
    }

    private static List<MutablePositionSnapshot> loadPositionsFromCSV() throws IOException {
        List<MutablePositionSnapshot> result = new ArrayList<>();

        File limitsDir = new File(EmberAppInfo.HOME_DIR, "positions");
        if (limitsDir.exists()) {
            File[] limitFiles = limitsDir.listFiles((file) -> file.getName().endsWith(".csv"));
            if (limitFiles != null) {
                System.err.println ("Found " + limitFiles.length + " files in positions directory");
                for (File limitFile : limitFiles)
                    loadPositionsFromCSV(limitFile, result);
            }
        } else {
            System.err.println ("Directory $EMBER_HOME/positions does not exist");
        }
        return result;
    }

    private static void loadPositionsFromCSV(File limitFile, List<MutablePositionSnapshot> result) throws IOException {
        CSVXReader csv = new CSVXReader(limitFile, ',');

        csv.readHeaders();
        List<ProjectionKey> projectionKeys = parseHeader(csv.getHeaders());

        MutablePositionSnapshot request = null;
        AsciiStringBuilder projectionPath = new AsciiStringBuilder();
        while (csv.nextLine()) {
            if (csv.getLine().trim().isEmpty())
                continue;
            int cellIndex = 0;


            // Position projection path (e.g. "Source[FIXTRADER1]/Account[GOLD]"). Empty for root level.
            projectionPath.clear();
            while (cellIndex < projectionKeys.size()) {
                if (cellIndex > 0)
                    projectionPath.append('/');
                projectionPath.append(projectionKeys.get(cellIndex));
                projectionPath.append('[');
                projectionPath.append(csv.getString(cellIndex).trim());
                projectionPath.append(']');

                cellIndex++;
            }

            request = new MutablePositionSnapshot();
            request.setSourceId(CLIENT_SOURCE_ID);
            request.setTimestamp(System.currentTimeMillis());
            request.setRelative(false);
            request.setProjectionPath(projectionPath.toString()); // clone it

            //TODO: read averageCost and realizedPnL ?

            String size = csv.getString(cellIndex).trim();
            request.setSize(Decimal64Utils.parse(size));

            result.add(request);

        }

        if (request != null)
            request.setLast(true);

        csv.close();
    }


    /** Reads projection keys used by this CSV file from the header (e.g.  Trader,Destination,Currency,Size ) */
    private static List<ProjectionKey> parseHeader(String [] header) {
        List<ProjectionKey> result = new ArrayList<>();


        for (int i=0; i < header.length - 1; i++) {
            String column = header[i].trim();

            try {
                result.add(ProjectionKey.valueOf(column));
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                break;
            }
        }

        if (result.isEmpty())
            throw new IllegalArgumentException("CSV Header is empty");

        String lastColumn = header[header.length - 1].trim();
        if ( ! lastColumn.equals("Size"))
            throw new IllegalArgumentException("The last column in the header must be \"Size\", instead got: " + lastColumn);


        return result;
    }
}
