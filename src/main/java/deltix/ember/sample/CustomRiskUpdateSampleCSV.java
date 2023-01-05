package deltix.ember.sample;

import deltix.ember.app.EmberAppInfo;
import deltix.ember.message.risk.*;
import deltix.ember.service.oms.position.Projection;
import deltix.ember.service.oms.position.ProjectionPath;
import deltix.ember.service.oms.risk.api.RiskLimitDefinition;
//import deltix.ember.service.oms.risk.api.RiskTableDefinition;
//import deltix.ember.service.oms.risk.limits.LimitDefinitions;
import deltix.util.collections.generated.ObjectArrayList;
import deltix.util.csvx.CSVXReader;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads Custom Risk Limits from CSV file under $(ember.home)/risklimits directory.
 *
 * Similar to CustomRiskUpdateSampleCSV but supports custom risk limits (by allowing limit names not listed in LimitDefinitions enum)
 */
public class CustomRiskUpdateSampleCSV extends SampleSupportTools {

    // We don't really need exact parameters on load side
    //private final static LimitDefinitions limitDefinitions = new LimitDefinitions(Duration.ofSeconds(1), false);


    public static void main(String[] args) throws InterruptedException {
        sendRequest(
                (publication) -> {

                    try {
                        for (RiskUpdateRequest request : loadRiskLimitsFromCSV()) {
                            publication.onRiskUpdateRequest(request);
                            System.out.println("Sent risk update request " + request);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    private static List<RiskUpdateRequest> loadRiskLimitsFromCSV() throws IOException {
        List<RiskUpdateRequest> result = new ArrayList<>();
        File limitsDir = new File(EmberAppInfo.HOME_DIR, "risklimits");
        if (limitsDir.exists()) {
            File[] limitFiles = limitsDir.listFiles((file) -> file.getName().endsWith(".csv"));
            if (limitFiles != null) {
                System.err.println("Found " + limitFiles.length + " file(s) in risklimits directory");
                for (File limitFile : limitFiles)
                    result.add(loadRiskLimitsFromCSV(limitFile));
            }
        } else {
            System.err.println("Directory $EMBER_HOME/risklimits does not exist");
        }
        return result;
    }

    private static long requestId;

    private static RiskUpdateRequest loadRiskLimitsFromCSV(File limitFile) throws IOException {
        CSVXReader csv = new CSVXReader(limitFile, ',');

        csv.readHeaders();

        String projection = parseProjectionFromHeader(csv.getHeaders());
        ProjectionKey [] projectionKeys = parseProjectionKeysFromHeader(csv.getHeaders());
        String [] limitNames = parseLimitNamesFromHeader(csv.getHeaders());

        MutableRiskUpdateRequest request = new MutableRiskUpdateRequest();
        request.setSourceId(CLIENT_SOURCE_ID);
        request.setTimestamp(System.currentTimeMillis());
        request.setChangedByUserId("riskmanager");
        request.setRequestId(Long.toString(++requestId));
        request.setProjection(projection);
        ObjectArrayList<RiskTableCommand> commands = new ObjectArrayList<>();
        request.setCommands(commands);
        while (csv.nextLine()) {
            if (csv.getLine().trim().isEmpty())
                continue;
            int cellIndex = 0;
            MutableRiskTableCommand command = new MutableRiskTableCommand();
            commands.add(command);
            command.setCmdType(RiskTableCommandType.UPDATE); // Update actually acts as INSERT-or-UPDATE

            ObjectArrayList<RiskCondition> conditions = new ObjectArrayList<>();
            command.setConditions(conditions);
            for (ProjectionKey projectionKey : projectionKeys) {
                MutableRiskCondition condition = new MutableRiskCondition();
                condition.setProjectionKey(projectionKey);
                condition.setValue(csv.getString(cellIndex++).trim());
                conditions.add(condition);
            }

            ObjectArrayList<RiskLimit> limits = new ObjectArrayList<>();
            command.setLimits(limits);
            for (String limitName : limitNames) {
                MutableRiskLimit limit = new MutableRiskLimit();
                String value = csv.getString(cellIndex++).trim();

                limit.setName(limitName);
                limit.setValue(value.isEmpty() ? null : value);
                limits.add(limit);
            }
        }

        csv.close();
        return request;
    }


    /**
     * @param header Case table definition. Starts with projection keys and ends with optional limit names. Something like  "Account,Symbol,MaxOrderSize"
     * @return projection (e.g. "Account/Symbol" )
     */
    private static String parseProjectionFromHeader (String[] header) {
        StringBuilder result = new StringBuilder();
        for (String column : header) {
            column = column.trim();
            try {
                ProjectionKey projectionKey = ProjectionKey.valueOf(column);
                if (result.length() > 0)
                    result.append(ProjectionPath.FIELD_PATH_SEPARATOR);
                result.append(projectionKey.name());
            } catch (IllegalArgumentException e) {
                break;
            }
        }
        return result.toString();
    }

    /**
     * @param header Case table definition. Starts with projection keys and ends with optional limit names. Something like  "Account,Symbol,MaxOrderSize"
     * @return projection (e.g. [ ProjectionKey.Account, ProjectionKey.Symbol ] )
     */
    private static ProjectionKey[] parseProjectionKeysFromHeader (String[] header) {
        List<ProjectionKey> result = new ArrayList<>();
        for (String column : header) {
            column = column.trim();
            try {
                result.add(ProjectionKey.valueOf(column));
            } catch (IllegalArgumentException e) {
                break;
            }
        }
        return result.toArray(new ProjectionKey[0]);
    }

    /**
     * @param header Case table definition. Starts with projection keys and ends with optional limit names. Something like  "Account,Symbol,MaxOrderSize"
     * @return projection (e.g. [ "MaxOrderSize" ] )
     */
    private static String[] parseLimitNamesFromHeader (String[] header) {
        List<String> result = new ArrayList<>();

        for (String column : header) {
            column = column.trim();
            try {
                ProjectionKey.valueOf(column);
            } catch (IllegalArgumentException e) {
                result.add(column); // for simplicity we will assume that everything that is not a ProjectionKey is a limit
            }
        }
        return result.toArray(new String[0]);
    }

}