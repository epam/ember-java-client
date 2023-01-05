package deltix.ember.sample;

import deltix.ember.message.risk.*;
import deltix.ember.service.oms.risk.api.RiskUtils;
import deltix.util.collections.generated.ObjectArrayList;

import javax.annotation.Nonnull;

/**
 * Programmatically control risk limits
 */
public class RiskUpdateSample extends SampleSupportTools {
    public static void main(String[] args) throws InterruptedException {
        sendRequest(
                (publication) -> {

                    MutableRiskUpdateRequest request = createRiskUpdateRequest();
                    publication.onRiskUpdateRequest(request);
                    System.out.println("Sent risk update request " + request);

                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    @Nonnull
    private static MutableRiskUpdateRequest createRiskUpdateRequest() {
        MutableRiskUpdateRequest request = new MutableRiskUpdateRequest();
        request.setSourceId(CLIENT_SOURCE_ID);
        request.setTimestamp(System.currentTimeMillis());
        request.setProjection("Account/Symbol");
        request.setChangedByUserId("riskmanager");
        request.setRequestId("123");

        ObjectArrayList<RiskTableCommand> commands = new ObjectArrayList<>();

        MutableRiskTableCommand command1 = new MutableRiskTableCommand();
        command1.setCmdType(RiskTableCommandType.INSERT);
        command1.setConditions(RiskUtils.makeList(
                RiskUtils.makeCondition(ProjectionKey.Account, "GOLD"),
                RiskUtils.makeCondition(ProjectionKey.Symbol, "*")
        ));
        command1.setLimits(RiskUtils.makeList(
                RiskUtils.makeLimit("MaxOrderSize", "100"),
                RiskUtils.makeLimit("MaxPositionLong", "10000")
        ));
        commands.add(command1);
        request.setCommands(commands);
        return request;
    }
}
