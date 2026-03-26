package se.forsman.bevakning.app;

import se.forsman.bevakning.config.AppConfig;
import se.forsman.bevakning.repository.AcknowledgementRepository;
import se.forsman.bevakning.repository.AlertHistoryRepository;
import se.forsman.bevakning.repository.EditableMonitoringRuleRepository;
import se.forsman.bevakning.repository.FlowRepository;
import se.forsman.bevakning.repository.MonitoringRuleRepository;
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

        FlowRepository flowRepository = new MockFlowRepository();

        EditableMonitoringRuleRepository ruleRepository =
                new FileMonitoringRuleRepository(config.get("app.data.rules.path"));

        MonitoringRuleRepository monitoringRuleRepository = ruleRepository;
        MonitoringEngine monitoringEngine = new MonitoringEngine();
        DashboardService dashboardService = new DashboardService(flowRepository, monitoringRuleRepository, monitoringEngine);
        RuleService ruleService = new RuleService(ruleRepository);

        AcknowledgementRepository acknowledgementRepository =
                new FileAcknowledgementRepository(config.get("app.data.acks.path"));
        AlertHistoryRepository alertHistoryRepository =
                new FileAlertHistoryRepository(config.get("app.data.alerts.path"));
        AcknowledgementService acknowledgementService =
                new AcknowledgementService(acknowledgementRepository, alertHistoryRepository);

        return new AppServices(dashboardService, ruleService, acknowledgementService);
    }
}
