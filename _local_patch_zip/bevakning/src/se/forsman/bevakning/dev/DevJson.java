package se.forsman.bevakning.dev;

import se.forsman.bevakning.domain.AlertHistoryEntry;
import se.forsman.bevakning.domain.DashboardSnapshot;
import se.forsman.bevakning.domain.MonitoringRule;
import se.forsman.bevakning.domain.MonitoringWindow;
import se.forsman.bevakning.domain.RuleEvaluation;

import java.time.LocalDateTime;
import java.util.List;

public final class DevJson {
    private DevJson() {
    }

    public static String dashboard(DashboardSnapshot snapshot) {
        return dashboard(snapshot, LocalDateTime.now());
    }

    public static String dashboard(DashboardSnapshot snapshot, LocalDateTime refreshedAt) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"totalFlowsToday\":").append(snapshot.getTotalFlowsToday()).append(",");
        sb.append("\"ok\":").append(snapshot.getOk()).append(",");
        sb.append("\"info\":").append(snapshot.getInfo()).append(",");
        sb.append("\"warning\":").append(snapshot.getWarning()).append(",");
        sb.append("\"error\":").append(snapshot.getError()).append(",");
        sb.append("\"backendOk\":").append(snapshot.isBackendOk()).append(",");
        sb.append("\"backendMessage\":\"").append(escape(snapshot.getBackendMessage())).append("\",");
        sb.append("\"incomingStartedAt\":");
        if (snapshot.getIncomingStartedAt() == null) sb.append("null");
        else sb.append("\"").append(escape(snapshot.getIncomingStartedAt())).append("\"");
        sb.append(",");
        sb.append("\"outgoingStartedAt\":");
        if (snapshot.getOutgoingStartedAt() == null) sb.append("null");
        else sb.append("\"").append(escape(snapshot.getOutgoingStartedAt())).append("\"");
        sb.append(",");
        sb.append("\"refreshedAt\":\"").append(escape(refreshedAt == null ? "" : refreshedAt.toString())).append("\",");
        sb.append("\"rows\":[");

        for (int i = 0; i < snapshot.getRows().size(); i++) {
            RuleEvaluation row = snapshot.getRows().get(i);
            if (i > 0) {
                sb.append(",");
            }

            sb.append("{");
            sb.append("\"id\":\"").append(escape(row.getRule().getId())).append("\",");
            sb.append("\"sender\":\"").append(escape(row.getRule().getSender())).append("\",");
            sb.append("\"receiver\":\"").append(escape(row.getRule().getReceiver())).append("\",");
            sb.append("\"msgType\":\"").append(escape(row.getRule().getMsgType())).append("\",");
            sb.append("\"description\":\"").append(escape(row.getRule().getDescription())).append("\",");
            sb.append("\"scheduleType\":\"").append(escape(row.getRule().getScheduleType())).append("\",");
            sb.append("\"status\":\"").append(escape(row.getStatus().name())).append("\",");
            sb.append("\"countToday\":").append(row.getCountToday()).append(",");
            sb.append("\"deadline\":\"").append(escape(row.getCurrentDeadline())).append("\",");
            sb.append("\"message\":\"").append(escape(row.getMessage())).append("\",");

            sb.append("\"occurrenceKey\":\"").append(escape(row.getOccurrenceKey())).append("\",");
            sb.append("\"acknowledged\":").append(row.isAcknowledged()).append(",");
            sb.append("\"acknowledgedBy\":\"").append(escape(row.getAcknowledgedBy())).append("\",");
            sb.append("\"acknowledgedAt\":\"").append(escape(row.getAcknowledgedAt())).append("\"");

            sb.append("}");
        }

        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    public static String rules(List<MonitoringRule> rules) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < rules.size(); i++) {
            MonitoringRule rule = rules.get(i);
            if (i > 0) {
                sb.append(",");
            }

            sb.append("{");
            sb.append("\"id\":\"").append(escape(rule.getId())).append("\",");
            sb.append("\"sender\":\"").append(escape(rule.getSender())).append("\",");
            sb.append("\"receiver\":\"").append(escape(rule.getReceiver())).append("\",");
            sb.append("\"msgType\":\"").append(escape(rule.getMsgType())).append("\",");
            sb.append("\"description\":\"").append(escape(rule.getDescription())).append("\",");
            sb.append("\"scheduleType\":\"").append(escape(rule.getScheduleType())).append("\",");
            sb.append("\"active\":").append(rule.isActive()).append(",");
            sb.append("\"minExpected\":").append(rule.getMinExpected()).append(",");
            sb.append("\"maxExpected\":").append(rule.getMaxExpected()).append(",");
            sb.append("\"deadline\":\"").append(escape(rule.getDeadline())).append("\",");
            sb.append("\"warningMinutesBeforeDeadline\":").append(rule.getWarningMinutesBeforeDeadline()).append(",");
            sb.append("\"weekdays\":\"").append(escape(rule.getWeekdays())).append("\",");
            sb.append("\"monthDays\":\"").append(escape(rule.getMonthDays())).append("\",");
            sb.append("\"specificDates\":\"").append(escape(rule.getSpecificDates())).append("\",");
            sb.append("\"useHistoricalBaseline\":").append(rule.isUseHistoricalBaseline()).append(",");
            sb.append("\"historicalDays\":").append(rule.getHistoricalDays()).append(",");
            sb.append("\"minPercentOfAverage\":").append(rule.getMinPercentOfAverage()).append(",");
            sb.append("\"windowsSpec\":\"").append(escape(toWindowsSpec(rule.getWindows()))).append("\"");
            sb.append("}");
        }

        sb.append("]");
        return sb.toString();
    }

    public static String history(List<AlertHistoryEntry> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < rows.size(); i++) {
            AlertHistoryEntry row = rows.get(i);
            if (i > 0) {
                sb.append(",");
            }

            sb.append("{");
            sb.append("\"id\":\"").append(escape(row.getId())).append("\",");
            sb.append("\"ruleId\":\"").append(escape(row.getRuleId())).append("\",");
            sb.append("\"occurrenceKey\":\"").append(escape(row.getOccurrenceKey())).append("\",");
            sb.append("\"eventType\":\"").append(escape(row.getEventType())).append("\",");
            sb.append("\"status\":\"").append(escape(row.getStatus())).append("\",");
            sb.append("\"message\":\"").append(escape(row.getMessage())).append("\",");
            sb.append("\"createdAt\":\"").append(escape(row.getCreatedAt())).append("\",");
            sb.append("\"createdBy\":\"").append(escape(row.getCreatedBy())).append("\"");
            sb.append("}");
        }

        sb.append("]");
        return sb.toString();
    }

    public static String config(String appMode,
                                int refreshSeconds,
                                String db2JndiName,
                                String db2QueryMode,
                                String db2Schema,
                                int db2LookbackDays,
                                String sourceDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"appMode\":\"").append(escape(appMode)).append("\",");
        sb.append("\"refreshSeconds\":").append(refreshSeconds).append(",");
        sb.append("\"db2JndiName\":\"").append(escape(db2JndiName)).append("\",");
        sb.append("\"db2QueryMode\":\"").append(escape(db2QueryMode)).append("\",");
        sb.append("\"db2Schema\":\"").append(escape(db2Schema)).append("\",");
        sb.append("\"db2LookbackDays\":").append(db2LookbackDays).append(",");
        sb.append("\"sourceDescription\":\"").append(escape(sourceDescription)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    public static String okMessage(String message) {
        return "{\"message\":\"" + escape(message) + "\"}";
    }

    public static String message(String key, String value) {
        return "{\"" + escape(key) + "\":\"" + escape(value) + "\"}";
    }

    public static String error(String message) {
        return message("error", message == null ? "Tekniskt fel" : message);
    }

    public static String health(String mode, String source) {
        return "{"
                + "\"status\":\"UP\","
                + "\"mode\":\"" + escape(mode) + "\","
                + "\"source\":\"" + escape(source) + "\""
                + "}";
    }

    private static String toWindowsSpec(List<MonitoringWindow> windows) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < windows.size(); i++) {
            MonitoringWindow window = windows.get(i);
            if (i > 0) {
                sb.append("\n");
            }
            sb.append(window.getDeadline())
              .append("|")
              .append(window.getMinExpected())
              .append("|")
              .append(window.getMaxExpected());
        }
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
