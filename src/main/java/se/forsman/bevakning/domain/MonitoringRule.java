package se.forsman.bevakning.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MonitoringRule {
    private final String id;
    private final String sender;
    private final String receiver;
    private final String msgType;
    private final String description;
    private final String scheduleType;
    private final boolean active;
    private final int minExpected;
    private final int maxExpected;
    private final String deadline;
    private final int warningMinutesBeforeDeadline;
    private final List<MonitoringWindow> windows;

    public MonitoringRule(String id,
                          String sender,
                          String receiver,
                          String msgType,
                          String description,
                          String scheduleType,
                          boolean active,
                          int minExpected,
                          int maxExpected,
                          String deadline,
                          int warningMinutesBeforeDeadline,
                          List<MonitoringWindow> windows) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
        this.msgType = msgType;
        this.description = description;
        this.scheduleType = scheduleType;
        this.active = active;
        this.minExpected = minExpected;
        this.maxExpected = maxExpected;
        this.deadline = deadline;
        this.warningMinutesBeforeDeadline = warningMinutesBeforeDeadline;
        this.windows = windows == null ? new ArrayList<MonitoringWindow>() : new ArrayList<MonitoringWindow>(windows);
    }

    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMsgType() {
        return msgType;
    }

    public String getDescription() {
        return description;
    }

    public String getScheduleType() {
        return scheduleType;
    }

    public boolean isActive() {
        return active;
    }

    public int getMinExpected() {
        return minExpected;
    }

    public int getMaxExpected() {
        return maxExpected;
    }

    public String getDeadline() {
        return deadline;
    }

    public int getWarningMinutesBeforeDeadline() {
        return warningMinutesBeforeDeadline;
    }

    public List<MonitoringWindow> getWindows() {
        return Collections.unmodifiableList(windows);
    }

    public FlowKey toKey() {
        return new FlowKey(sender, receiver, msgType);
    }
}
