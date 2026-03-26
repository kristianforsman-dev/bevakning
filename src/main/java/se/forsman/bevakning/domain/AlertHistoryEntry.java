package se.forsman.bevakning.domain;

public class AlertHistoryEntry {
    private final String id;
    private final String ruleId;
    private final String eventType;
    private final String status;
    private final String message;
    private final String createdAt;
    private final String createdBy;

    public AlertHistoryEntry(String id, String ruleId, String eventType, String status, String message, String createdAt, String createdBy) {
        this.id = id;
        this.ruleId = ruleId;
        this.eventType = eventType;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }

    public String getId() {
        return id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }
}
