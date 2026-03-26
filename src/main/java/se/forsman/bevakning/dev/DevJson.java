package se.forsman.bevakning.dev;

import se.forsman.bevakning.domain.AlertHistoryEntry;
import se.forsman.bevakning.domain.DashboardSnapshot;
import se.forsman.bevakning.domain.MonitoringRule;
import se.forsman.bevakning.domain.MonitoringWindow;
import se.forsman.bevakning.domain.RuleEvaluation;
import se.forsman.bevakning.web.json.JsonResponseUtils;

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
        sb.append("\"backendMessage\":\"").append(JsonResponseUtils.escape(snapshot.getBackendMessage())).append("\",");
        sb.append("\"refreshedAt\":\"").append(JsonResponseUtils.escape(refreshedAt == null ? "" : refreshedAt.toString())).append("\",");
        sb.append("\"rows\":[");
        for (int i = 0; i < snapshot.getRows().size(); i++) {
            RuleEvaluation row = snapshot.getRows().get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"id\":\"").append(JsonResponseUtils.escape(row.getRule().getId())).append("\",");
            sb.append("\"sender\":\"").append(JsonResponseUtils.escape(row.getRule().getSender())).append("\",");
            sb.append("\"receiver\":\"").append(JsonResponseUtils.escape(row.getRule().getReceiver())).append("\",");
            sb.append("\"msgType\":\"").append(JsonResponseUtils.escape(row.getRule().getMsgType())).append("\",");
            sb.append("\"description\":\"").append(JsonResponseUtils.escape(row.getRule().getDescription())).append("\",");
            sb.append("\"scheduleType\":\"").append(JsonResponseUtils.escape(row.getRule().getScheduleType())).append("\",");
            sb.append("\"status\":\"").append(JsonResponseUtils.escape(row.getStatus().name())).append("\",");
            sb.append("\"countToday\":").append(row.getCountToday()).append(",");
            sb.append("\"deadline\":\"").append(JsonResponseUtils.escape(row.getCurrentDeadline())).append("\",");
            sb.append("\"message\":\"").append(JsonResponseUtils.escape(row.getMessage())).append("\"");
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
            sb.append("\"id\":\"").append(JsonResponseUtils.escape(r.getId())).append("\",");
            sb.append("\"sender\":\"").append(JsonResponseUtils.escape(r.getSender())).append("\",");
            sb.append("\"receiver\":\"").append(JsonResponseUtils.escape(r.getReceiver())).append("\",");
            sb.append("\"msgType\":\"").append(JsonResponseUtils.escape(r.getMsgType())).append("\",");
            sb.append("\"description\":\"").append(JsonResponseUtils.escape(r.getDescription())).append("\",");
            sb.append("\"scheduleType\":\"").append(JsonResponseUtils.escape(r.getScheduleType())).append("\",");
            sb.append("\"active\":").append(r.isActive()).append(",");
            sb.append("\"minExpected\":").append(r.getMinExpected()).append(",");
            sb.append("\"maxExpected\":").append(r.getMaxExpected()).append(",");
            sb.append("\"deadline\":\"").append(JsonResponseUtils.escape(r.getDeadline())).append("\",");
            sb.append("\"warningMinutesBeforeDeadline\":").append(r.getWarningMinutesBeforeDeadline()).append(",");
            sb.append("\"weekdays\":\"").append(JsonResponseUtils.escape(r.getWeekdays())).append("\",");
            sb.append("\"monthDays\":\"").append(JsonResponseUtils.escape(r.getMonthDays())).append("\",");
            sb.append("\"specificDates\":\"").append(JsonResponseUtils.escape(r.getSpecificDates())).append("\",");
            sb.append("\"useHistoricalBaseline\":").append(r.isUseHistoricalBaseline()).append(",");
            sb.append("\"historicalDays\":").append(r.getHistoricalDays()).append(",");
            sb.append("\"minPercentOfAverage\":").append(r.getMinPercentOfAverage()).append(",");
            sb.append("\"windowsSpec\":\"").append(JsonResponseUtils.escape(toWindowsSpec(r.getWindows()))).append("\"");
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
            sb.append("\"id\":\"").append(JsonResponseUtils.escape(row.getId())).append("\",");
            sb.append("\"ruleId\":\"").append(JsonResponseUtils.escape(row.getRuleId())).append("\",");
            sb.append("\"eventType\":\"").append(JsonResponseUtils.escape(row.getEventType())).append("\",");
            sb.append("\"status\":\"").append(JsonResponseUtils.escape(row.getStatus())).append("\",");
            sb.append("\"message\":\"").append(JsonResponseUtils.escape(row.getMessage())).append("\",");
            sb.append("\"createdAt\":\"").append(JsonResponseUtils.escape(row.getCreatedAt())).append("\",");
            sb.append("\"createdBy\":\"").append(JsonResponseUtils.escape(row.getCreatedBy())).append("\"");
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    public static String health(String mode, String source) {
        return JsonResponseUtils.health("UP", mode, source);
    }

    public static String error(String message) {
        return JsonResponseUtils.error(message);
    }

    public static String okMessage(String message) {
        return JsonResponseUtils.message(message);
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
}
