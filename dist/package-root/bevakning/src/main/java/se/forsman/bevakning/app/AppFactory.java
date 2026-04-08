package se.forsman.bevakning.app;

import se.forsman.bevakning.config.AppConfig;
import se.forsman.bevakning.repository.AcknowledgementRepository;
import se.forsman.bevakning.repository.AlertHistoryRepository;
import se.forsman.bevakning.repository.EditableMonitoringRuleRepository;
import se.forsman.bevakning.repository.FlowRepository;
import se.forsman.bevakning.repository.MonitoringRuleRepository;
import se.forsman.bevakning.repository.db2.Db2FlowRepository;
import se.forsman.bevakning.repository.mock.FileAcknowledgementRepository;
import se.forsman.bevakning.repository.mock.FileAlertHistoryRepository;
import se.forsman.bevakning.repository.mock.FileMonitoringRuleRepository;
import se.forsman.bevakning.repository.mock.MockFlowRepository;
import se.forsman.bevakning.service.AcknowledgementService;
import se.forsman.bevakning.service.DashboardService;
import se.forsman.bevakning.service.MonitoringEngine;
import se.forsman.bevakning.service.RuleService;

public final class AppFactory {
    private AppFactory() {
    }

    public static AppServices create() {
        AppConfig config = new AppConfig();

        String mode = config.get("app.mode", "mock").trim().toLowerCase();

        FlowRepository flowRepository;
        String sourceDescription;

        if ("db2".equalsIgnoreCase(mode)) {
            Db2FlowRepository db2Repository = new Db2FlowRepository(
                    config.get("db2.jndi.name", "jdbc/bevakningDb"),
                    config.getInt("db2.lookback.days", 14),
                    config.get("db2.schema", ""),
                    config.get("db2.query.mode", "PROCESS_ATTRIBUTE"),
                    config.get("db2.process.table", "PROCESS_INSTANCE"),
                    config.get("db2.attribute.table", "PROCESS_ATTRIBUTE"),
                    config.get("db2.b.table", "PROCESS_INSTANCE_B_"),
                    config.get("db2.b.attribute.table", "PROCESS_INSTANCE_ATTRIBUTE_T"),
                    config.get("db2.attribute.sender.key", "sender"),
                    config.get("db2.attribute.receiver.key", "receiver"),
                    config.get("db2.attribute.msgtype.key", "msgType")
            );
            flowRepository = db2Repository;
            sourceDescription = db2Repository.describeSource();
        } else {
            flowRepository = new MockFlowRepository();
            sourceDescription = "MockFlowRepository";
        }

        EditableMonitoringRuleRepository ruleRepository =
                new FileMonitoringRuleRepository(config.get("app.data.rules.path"));
        MonitoringRuleRepository monitoringRuleRepository = ruleRepository;

        AcknowledgementRepository acknowledgementRepository =
                new FileAcknowledgementRepository(config.get("app.data.acks.path"));
        AlertHistoryRepository alertHistoryRepository =
                new FileAlertHistoryRepository(config.get("app.data.alerts.path"));

        MonitoringEngine monitoringEngine = new MonitoringEngine();

        DashboardService dashboardService = new DashboardService(
                flowRepository,
                monitoringRuleRepository,
                alertHistoryRepository,
                acknowledgementRepository,
                monitoringEngine,
                config.getInt("db2.lookback.days", 14)
        );

        RuleService ruleService = new RuleService(ruleRepository);

        AcknowledgementService acknowledgementService =
                new AcknowledgementService(acknowledgementRepository, alertHistoryRepository);

        return new AppServices(
                dashboardService,
                ruleService,
                acknowledgementService,
                mode,
                sourceDescription
        );
    }
}
