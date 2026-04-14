package se.forsman.bevakning.app;

import se.forsman.bevakning.service.AcknowledgementService;
import se.forsman.bevakning.service.DashboardService;
import se.forsman.bevakning.service.RuleService;

public class AppServices {
    private final DashboardService dashboardService;
    private final RuleService ruleService;
    private final AcknowledgementService acknowledgementService;
    private final String mode;
    private final String sourceDescription;

    public AppServices(DashboardService dashboardService,
                       RuleService ruleService,
                       AcknowledgementService acknowledgementService,
                       String mode,
                       String sourceDescription) {
        this.dashboardService = dashboardService;
        this.ruleService = ruleService;
        this.acknowledgementService = acknowledgementService;
        this.mode = mode;
        this.sourceDescription = sourceDescription;
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

    public String getMode() {
        return mode;
    }

    public String getSourceDescription() {
        return sourceDescription;
    }
}
