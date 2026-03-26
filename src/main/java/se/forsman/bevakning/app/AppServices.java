package se.forsman.bevakning.app;

import se.forsman.bevakning.service.AcknowledgementService;
import se.forsman.bevakning.service.DashboardService;
import se.forsman.bevakning.service.RuleService;

public class AppServices {
    private final DashboardService dashboardService;
    private final RuleService ruleService;
    private final AcknowledgementService acknowledgementService;

    public AppServices(DashboardService dashboardService,
                       RuleService ruleService,
                       AcknowledgementService acknowledgementService) {
        this.dashboardService = dashboardService;
        this.ruleService = ruleService;
        this.acknowledgementService = acknowledgementService;
    }

    public DashboardService getDashboardService() {
        return dashboardService;
    }

    public RuleService getRuleService() {
        return ruleService;
    }

    public AcknowledgementService getAcknowledgementService() {
        return acknowledgementService;
    }
}
