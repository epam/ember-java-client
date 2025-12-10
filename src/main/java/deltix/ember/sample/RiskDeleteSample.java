package deltix.ember.sample;

import deltix.anvil.util.ShutdownSignal;
import deltix.ember.message.risk.*;
import com.epam.deltix.gflog.api.Log;
import com.epam.deltix.gflog.api.LogFactory;
import deltix.util.collections.generated.ObjectArrayList;
import deltix.util.collections.generated.ObjectList;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** This sample lists all risk limits for Account/Symbol projection and deletes all rows where Account=GOLD */
public class RiskDeleteSample extends SampleSupportTools {

    private static final Log LOGGER = LogFactory.getLog(RiskDeleteSample.class);
    private static final String PROJECTION = "Account/Symbol";
    private static final String TARGET_ACCOUNT = "GOLD";

    private static class RowToDelete {
        final ObjectList<RiskCondition> conditions;
        final ObjectList<RiskLimit> limits;

        RowToDelete(ObjectList<RiskCondition> conditions, ObjectList<RiskLimit> limits) {
            this.conditions = conditions;
            this.limits = limits;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        final ShutdownSignal snapshotSignal = new ShutdownSignal();
        final List<RowToDelete> rowsToDelete = new ArrayList<>();
        final String[] snapshotRequestId = new String[1]; // Use array to make it effectively final

        // Step 1: List all risk limits
        sendRequest(
                (publication) -> {
                    snapshotRequestId[0] = "RTS#" + System.currentTimeMillis();
                    MutableRiskTableSnapshotRequest snapshotRequest = createRiskTableSnapshotRequest(snapshotRequestId[0], PROJECTION);
                    publication.onRiskTableSnapshotRequest(snapshotRequest);
                    LOGGER.info("Sent risk table snapshot request %s for projection %s").with(snapshotRequestId[0]).with(PROJECTION);

                    if (snapshotSignal.await(5, TimeUnit.MINUTES)) {
                        LOGGER.info("Snapshot request completed. Found %s rows to delete").with(rowsToDelete.size());
                    } else {
                        LOGGER.error("Timeout waiting for risk table snapshot response!");
                        return;
                    }
                },

                (message) -> {
                    if (message instanceof RiskTableSnapshotResponse) {
                        RiskTableSnapshotResponse response = (RiskTableSnapshotResponse) message;
                        if (snapshotRequestId[0] != null && response.getRequestId().equals(snapshotRequestId[0])) {
                            handleSnapshotResponse(response, rowsToDelete, snapshotSignal);
                        }
                    }
                }
        );

        if (rowsToDelete.isEmpty()) {
            LOGGER.info("No rows found with Account=%s. Nothing to delete.").with(TARGET_ACCOUNT);
            return;
        }

        // Step 2: Delete all matching rows
        final ShutdownSignal deleteSignal = new ShutdownSignal();
        final String[] deleteRequestId = new String[1]; // Use array to make it effectively final
        sendRequest(
                (publication) -> {
                    deleteRequestId[0] = "RUD#" + System.currentTimeMillis();
                    MutableRiskUpdateRequest deleteRequest = createDeleteRequest(deleteRequestId[0], rowsToDelete);
                    publication.onRiskUpdateRequest(deleteRequest);
                    LOGGER.info("Sent risk update request %s to delete %s rows").with(deleteRequestId[0]).with(rowsToDelete.size());

                    if (deleteSignal.await(5, TimeUnit.MINUTES)) {
                        LOGGER.info("Delete request completed successfully");
                    } else {
                        LOGGER.error("Timeout waiting for risk update response!");
                    }
                },

                (message) -> {
                    if (message instanceof RiskUpdateResponse) {
                        RiskUpdateResponse response = (RiskUpdateResponse) message;
                        if (deleteRequestId[0] != null && response.getRequestId().equals(deleteRequestId[0])) {
                            handleUpdateResponse(response, deleteSignal);
                        }
                    }
                }
        );
    }

    private static void handleSnapshotResponse(RiskTableSnapshotResponse response, List<RowToDelete> rowsToDelete, ShutdownSignal signal) {
        if (!response.isSuccess()) {
            LOGGER.error("Snapshot request failed: %s").with(response.getErrorMessage());
            signal.signal();
            return;
        }

        if (response.hasConditions() && response.hasLimits()) {
            ObjectList<RiskCondition> conditions = response.getConditions();
            ObjectList<RiskLimit> limits = response.getLimits();
            // Check if Account condition matches TARGET_ACCOUNT
            for (int i = 0; i < conditions.size(); i++) {
                RiskCondition condition = conditions.get(i);
                if (condition.getProjectionKey() == ProjectionKey.Account) {
                    CharSequence accountValue = condition.getValue();
                    if (accountValue != null && accountValue.toString().equals(TARGET_ACCOUNT)) {
                        // Found a matching row - copy both conditions and limits
                        ObjectArrayList<RiskCondition> conditionsCopy = new ObjectArrayList<>(conditions.size());
                        for (int j = 0; j < conditions.size(); j++) {
                            RiskCondition cond = conditions.get(j);
                            MutableRiskCondition condCopy = new MutableRiskCondition();
                            condCopy.setProjectionKey(cond.getProjectionKey());
                            condCopy.setValue(cond.getValue());
                            conditionsCopy.add(condCopy);
                        }
                        ObjectArrayList<RiskLimit> limitsCopy = new ObjectArrayList<>(limits.size());
                        for (int j = 0; j < limits.size(); j++) {
                            RiskLimit limit = limits.get(j);
                            MutableRiskLimit limitCopy = new MutableRiskLimit();
                            limitCopy.setName(limit.getName());
                            limitCopy.setValue(limit.getValue());
                            limitsCopy.add(limitCopy);
                        }
                        rowsToDelete.add(new RowToDelete(conditionsCopy, limitsCopy));
                        LOGGER.info("Found row to delete: Account=%s, Symbol=%s")
                                .with(accountValue)
                                .with(getSymbolValue(conditions));
                        break;
                    }
                }
            }
        }

        if (response.isLast()) {
            signal.signal();
        }
    }

    private static String getSymbolValue(ObjectList<RiskCondition> conditions) {
        for (int i = 0; i < conditions.size(); i++) {
            RiskCondition condition = conditions.get(i);
            if (condition.getProjectionKey() == ProjectionKey.Symbol) {
                CharSequence value = condition.getValue();
                return (value != null) ? value.toString() : "";
            }
        }
        return "";
    }

    private static void handleUpdateResponse(RiskUpdateResponse response, ShutdownSignal signal) {
        if (response.isSuccess()) {
            LOGGER.info("Risk update request %s succeeded").with(response.getRequestId());
        } else {
            LOGGER.error("Risk update request %s failed: %s")
                    .with(response.getRequestId())
                    .with(response.getErrorMessage());
        }
        signal.signal();
    }

    @Nonnull
    private static MutableRiskTableSnapshotRequest createRiskTableSnapshotRequest(String requestId, String projection) {
        MutableRiskTableSnapshotRequest request = new MutableRiskTableSnapshotRequest();
        request.setRequestId(requestId);
        request.setTimestamp(System.currentTimeMillis());
        request.setSourceId(CLIENT_SOURCE_ID);
        request.setProjection(projection);
        return request;
    }

    @Nonnull
    private static MutableRiskUpdateRequest createDeleteRequest(String requestId, List<RowToDelete> rowsToDelete) {
        MutableRiskUpdateRequest request = new MutableRiskUpdateRequest();
        request.setRequestId(requestId);
        request.setTimestamp(System.currentTimeMillis());
        request.setSourceId(CLIENT_SOURCE_ID);
        request.setProjection(PROJECTION);
        request.setChangedByUserId("riskmanager");

        ObjectArrayList<RiskTableCommand> commands = new ObjectArrayList<>();
        for (RowToDelete row : rowsToDelete) {
            MutableRiskTableCommand command = new MutableRiskTableCommand();
            command.setCmdType(RiskTableCommandType.DELETE);
            command.setConditions(row.conditions);
            // Limits are required for DELETE - copy from snapshot response
            command.setLimits(row.limits);
            commands.add(command);
        }
        request.setCommands(commands);
        return request;
    }
}

