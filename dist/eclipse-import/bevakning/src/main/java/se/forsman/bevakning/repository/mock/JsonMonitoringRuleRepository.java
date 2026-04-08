package se.forsman.bevakning.repository.mock;

import se.forsman.bevakning.domain.MonitoringRule;
import se.forsman.bevakning.domain.MonitoringWindow;
import se.forsman.bevakning.repository.MonitoringRuleRepository;
import se.forsman.bevakning.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonMonitoringRuleRepository implements MonitoringRuleRepository {
    @Override
    public List<MonitoringRule> findAllRules() {
        List<Map<String, Object>> rows = castList(JsonUtils.readResource("data/monitoring-rules.json"));
        return toRules(rows);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    private List<MonitoringRule> toRules(List<Map<String, Object>> rows) {
        List<MonitoringRule> result = new ArrayList<MonitoringRule>();

        for (Map<String, Object> row : rows) {
            List<MonitoringWindow> windows = new ArrayList<MonitoringWindow>();
            Object windowsObj = row.get("windows");
            if (windowsObj instanceof List) {
                List<Map<String, Object>> list = castList(windowsObj);
                for (Map<String, Object> windowRow : list) {
                    windows.add(new MonitoringWindow(
                            stringValue(windowRow.get("deadline")),
                            toInt(windowRow.get("minExpected")),
                            toInt(windowRow.get("maxExpected"))
                    ));
                }
            }

            result.add(new MonitoringRule(
                    stringValue(row.get("id")),
                    stringValue(row.get("sender")),
                    stringValue(row.get("receiver")),
                    stringValue(row.get("msgType")),
                    stringValue(row.get("description")),
                    stringValue(row.get("scheduleType")),
                    toBoolean(row.get("active")),
                    toInt(row.get("minExpected")),
                    toInt(row.get("maxExpected")),
                    stringValue(row.get("deadline")),
                    toInt(row.get("warningMinutesBeforeDeadline")),
                    windows,
                    stringValue(row.get("weekdays")),
                    stringValue(row.get("monthDays")),
                    stringValue(row.get("specificDates")),
                    toBoolean(row.get("useHistoricalBaseline")),
                    toInt(row.get("historicalDays")),
                    toInt(row.get("minPercentOfAverage"))
            ));
        }

        return result;
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(String.valueOf(value));
    }

    private boolean toBoolean(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return ((Boolean) value).booleanValue();
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
