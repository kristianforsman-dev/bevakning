package se.forsman.bevakning.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardSnapshot {
    private final int totalFlowsToday;
    private final int ok;
    private final int info;
    private final int warning;
    private final int error;
    private final List<RuleEvaluation> rows;
    private final boolean backendOk;
    private final String backendMessage;

        private final String incomingStartedAt;
    private final String outgoingStartedAt;

public DashboardSnapshot(int totalFlowsToday,
                             int ok,
                             int info,
                             int warning,
                             int error,
                             List<RuleEvaluation> rows,
                             boolean backendOk,
                             String backendMessage, String incomingStartedAt, String outgoingStartedAt) {
        this.totalFlowsToday = totalFlowsToday;
        this.ok = ok;
        this.info = info;
        this.warning = warning;
        this.error = error;
        this.rows = rows == null ? new ArrayList<RuleEvaluation>() : new ArrayList<RuleEvaluation>(rows);
        this.backendOk = backendOk;
        this.backendMessage = backendMessage;
        this.incomingStartedAt = incomingStartedAt;
        this.outgoingStartedAt = outgoingStartedAt;
    }

    public int getTotalFlowsToday() {
        return totalFlowsToday;
    }

    public int getOk() {
        return ok;
    }

    public int getInfo() {
        return info;
    }

    public int getWarning() {
        return warning;
    }

    public int getError() {
        return error;
    }

    public List<RuleEvaluation> getRows() {
        return Collections.unmodifiableList(rows);
    }

    public boolean isBackendOk() {
        return backendOk;
    }

    public String getBackendMessage() {
        return backendMessage;
    }

    public String getIncomingStartedAt() {
        return incomingStartedAt;
    }

    public String getOutgoingStartedAt() {
        return outgoingStartedAt;
    }
}
