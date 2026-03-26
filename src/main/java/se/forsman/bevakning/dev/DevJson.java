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
        sb.append("\"refreshedAt\":\"").append(escape(refreshedAt == null ? "" : refreshedAt.toString())).append("\",");
        sb.append("\"rows\":[");
        for (int i = 0; i < snapshot.getRows().size(); i++) {
            RuleEvaluation row = snapshot.getRows().get(i);
            if (i > 0) sb.append(",");
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
            sb.append("\"message\":\"").append(escape(row.getMessage())).append("\"");
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
            MonitoringRule r = rules.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"id\":\"").append(escape(r.getId())).append("\",");
            sb.append("\"sender\":\"").append(escape(r.getSender())).append("\",");
            sb.append("\"receiver\":\"").append(escape(r.getReceiver())).append("\",");
            sb.append("\"msgType\":\"").append(escape(r.getMsgType())).append("\",");
            sb.append("\"description\":\"").append(escape(r.getDescription())).append("\",");
            sb.append("\"scheduleType\":\"").append(escape(r.getScheduleType())).append("\",");
            sb.append("\"active\":").append(r.isActive()).append(",");
            sb.append("\"minExpected\":").append(r.getMinExpected()).append(",");
            sb.append("\"maxExpected\":").append(r.getMaxExpected()).append(",");
            sb.append("\"deadline\":\"").append(escape(r.getDeadline())).append("\",");
            sb.append("\"warningMinutesBeforeDeadline\":").append(r.getWarningMinutesBeforeDeadline()).append(",");
            sb.append("\"weekdays\":\"").append(escape(r.getWeekdays())).append("\",");
            sb.append("\"monthDays\":\"").append(escape(r.getMonthDays())).append("\",");
            sb.append("\"specificDates\":\"").append(escape(r.getSpecificDates())).append("\",");
            sb.append("\"useHistoricalBaseline\":").append(r.isUseHistoricalBaseline()).append(",");
            sb.append("\"historicalDays\":").append(r.getHistoricalDays()).append(",");
            sb.append("\"minPercentOfAverage\":").append(r.getMinPercentOfAverage()).append(",");
            sb.append("\"windowsSpec\":\"").append(escape(toWindowsSpec(r.getWindows()))).append("\"");
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
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"id\":\"").append(escape(row.getId())).append("\",");
            sb.append("\"ruleId\":\"").append(escape(row.getRuleId())).append("\",");
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

    public static String health(String mode, String source) {
        return "{\"status\":\"UP\",\"mode\":\"" + escape(mode) + "\",\"source\":\"" + escape(source) + "\"}";
    }

    public static String error(String message) {
        return "{\"status\":\"ERROR\",\"message\":\"" + escape(message) + "\"}";
    }

    public static String okMessage(String message) {
        return "{\"message\":\"" + escape(message) + "\"}";
    }

    private static String toWindowsSpec(List<MonitoringWindow> windows) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < windows.size(); i++) {
            MonitoringWindow w = windows.get(i);
            if (i > 0) sb.append("\n");
            sb.append(w.getDeadline()).append("|").append(w.getMinExpected()).append("|").append(w.getMaxExpected());
        }
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
