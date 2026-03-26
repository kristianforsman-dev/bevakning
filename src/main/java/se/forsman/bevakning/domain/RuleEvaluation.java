package se.forsman.bevakning.domain;

public class RuleEvaluation {
    private final MonitoringRule rule;
    private final AlertStatus status;
    private final int countToday;
    private final String currentDeadline;
    private final String message;

    public RuleEvaluation(MonitoringRule rule, AlertStatus status, int countToday, String currentDeadline, String message) {
        this.rule = rule;
        this.status = status;
        this.countToday = countToday;
        this.currentDeadline = currentDeadline;
        this.message = message;
    }

    public MonitoringRule getRule() {
        return rule;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public int getCountToday() {
        return countToday;
    }

    public String getCurrentDeadline() {
        return currentDeadline;
    }

    public String getMessage() {
        return message;
    }
}
