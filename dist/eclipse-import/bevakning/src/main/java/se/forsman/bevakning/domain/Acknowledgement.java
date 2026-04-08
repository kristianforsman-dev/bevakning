package se.forsman.bevakning.domain;

public class Acknowledgement {
    private final String id;
    private final String ruleId;
    private final String occurrenceKey;
    private final String status;
    private final String comment;
    private final String acknowledgedBy;
    private final String acknowledgedAt;

    public Acknowledgement(String id,
                           String ruleId,
                           String occurrenceKey,
                           String status,
                           String comment,
                           String acknowledgedBy,
                           String acknowledgedAt) {
        this.id = id;
        this.ruleId = ruleId;
        this.occurrenceKey = occurrenceKey == null ? "" : occurrenceKey;
        this.status = status;
        this.comment = comment;
        this.acknowledgedBy = acknowledgedBy;
        this.acknowledgedAt = acknowledgedAt;
    }

    public String getId() {
        return id;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getOccurrenceKey() {
        return occurrenceKey;
    }

    public String getStatus() {
        return status;
    }

    public String getComment() {
        return comment;
    }

    public String getAcknowledgedBy() {
        return acknowledgedBy;
    }

    public String getAcknowledgedAt() {
        return acknowledgedAt;
    }
}
