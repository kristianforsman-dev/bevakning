package se.forsman.bevakning.domain;

public class RuleEvaluation {
    private final MonitoringRule rule;
    private final AlertStatus status;
    private final int countToday;
    private final String currentDeadline;
    private final String message;
    private final String occurrenceKey;
    private final boolean acknowledged;
    private final String acknowledgedBy;
    private final String acknowledgedAt;

    public RuleEvaluation(MonitoringRule rule,
                          AlertStatus status,
                          int countToday,
                          String currentDeadline,
                          String message) {
        this(rule, status, countToday, currentDeadline, message, "", false, "", "");
    }

    public RuleEvaluation(MonitoringRule rule,
                          AlertStatus status,
                          int countToday,
                          String currentDeadline,
                          String message,
                          String occurrenceKey,
                          boolean acknowledged,
                          String acknowledgedBy,
                          String acknowledgedAt) {
        this.rule = rule;
        this.status = status;
        this.countToday = countToday;
        this.currentDeadline = currentDeadline;
        this.message = message;
        this.occurrenceKey = occurrenceKey == null ? "" : occurrenceKey;
        this.acknowledged = acknowledged;
        this.acknowledgedBy = acknowledgedBy == null ? "" : acknowledgedBy;
        this.acknowledgedAt = acknowledgedAt == null ? "" : acknowledgedAt;
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

    public String getOccurrenceKey() {
        return occurrenceKey;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public String getAcknowledgedAt() {
        return acknowledgedAt;
    }
}
