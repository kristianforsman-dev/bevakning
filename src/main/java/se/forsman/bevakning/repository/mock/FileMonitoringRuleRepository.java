package se.forsman.bevakning.repository.mock;

import se.forsman.bevakning.domain.MonitoringRule;
import se.forsman.bevakning.domain.MonitoringWindow;
import se.forsman.bevakning.repository.EditableMonitoringRuleRepository;
import se.forsman.bevakning.util.FileJsonStore;
import se.forsman.bevakning.util.JsonUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FileMonitoringRuleRepository implements EditableMonitoringRuleRepository {
    private final String filePath;

    public FileMonitoringRuleRepository(String filePath) {
        this.filePath = filePath;
        FileJsonStore.copyResourceToFileIfMissing("data/monitoring-rules.json", filePath);
    }

    @Override
    public synchronized List<MonitoringRule> findAllRules() {
        Object parsed = JsonUtils.parseJson(FileJsonStore.readFile(filePath));
        List<Map<String, Object>> rows = castList(parsed);
        return toRules(rows);
    }

    @Override
    public synchronized void save(MonitoringRule rule) {
        List<MonitoringRule> rules = findAllRules();
        rules.add(rule);
        writeRules(rules);
    }

    @Override
    public synchronized void update(MonitoringRule rule) {
        List<MonitoringRule> rules = findAllRules();
        for (int i = 0; i < rules.size(); i++) {
            if (rules.get(i).getId().equals(rule.getId())) {
                rules.set(i, rule);
                writeRules(rules);
                return;
            }
        }
        throw new IllegalArgumentException("Regel finns inte: " + rule.getId());
    }

    @Override
    public synchronized void delete(String ruleId) {
        List<MonitoringRule> rules = findAllRules();
        Iterator<MonitoringRule> it = rules.iterator();
        while (it.hasNext()) {
            if (it.next().getId().equals(ruleId)) {
                it.remove();
            }
        }
        writeRules(rules);
    }

    @Override
    public synchronized MonitoringRule findById(String ruleId) {
        List<MonitoringRule> rules = findAllRules();
        for (MonitoringRule rule : rules) {
            if (rule.getId().equals(ruleId)) {
                return rule;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (value == null) {
            return new ArrayList<Map<String, Object>>();
        }

        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }

        if (value instanceof String) {
            Object reparsed = JsonUtils.parseJson((String) value);
            if (reparsed instanceof List) {
                return (List<Map<String, Object>>) reparsed;
            }
        }

        throw new IllegalStateException("Förväntade lista men fick: " + value.getClass().getName());
    }

    private List<MonitoringRule> toRules(List<Map<String, Object>> rows) {
        List<MonitoringRule> result = new ArrayList<MonitoringRule>();

        for (Map<String, Object> row : rows) {
            List<MonitoringWindow> windows = new ArrayList<MonitoringWindow>();
            Object windowsObj = row.get("windows");

            List<Map<String, Object>> windowRows = tryCastWindowList(windowsObj);
            for (Map<String, Object> windowRow : windowRows) {
                windows.add(new MonitoringWindow(
                        stringValue(windowRow.get("deadline")),
                        toInt(windowRow.get("minExpected")),
                        toInt(windowRow.get("maxExpected"))
                ));
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

    private List<Map<String, Object>> tryCastWindowList(Object value) {
        if (value == null) {
            return new ArrayList<Map<String, Object>>();
        }

        if (value instanceof List) {
            return castList(value);
        }

        if (value instanceof String) {
            String s = String.valueOf(value).trim();
            if (s.isEmpty()) {
                return new ArrayList<Map<String, Object>>();
            }

            Object reparsed = JsonUtils.parseJson(s);
            if (reparsed instanceof List) {
                return castList(reparsed);
            }
        }

        return new ArrayList<Map<String, Object>>();
    }

    private void writeRules(List<MonitoringRule> rules) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        for (int i = 0; i < rules.size(); i++) {
            MonitoringRule rule = rules.get(i);
            if (i > 0) sb.append(",\n");

            sb.append("  {\n");
            sb.append("    \"id\": \"").append(escape(rule.getId())).append("\",\n");
            sb.append("    \"sender\": \"").append(escape(rule.getSender())).append("\",\n");
            sb.append("    \"receiver\": \"").append(escape(rule.getReceiver())).append("\",\n");
            sb.append("    \"msgType\": \"").append(escape(rule.getMsgType())).append("\",\n");
            sb.append("    \"description\": \"").append(escape(rule.getDescription())).append("\",\n");
            sb.append("    \"scheduleType\": \"").append(escape(rule.getScheduleType())).append("\",\n");
            sb.append("    \"active\": ").append(rule.isActive()).append(",\n");
            sb.append("    \"minExpected\": ").append(rule.getMinExpected()).append(",\n");
            sb.append("    \"maxExpected\": ").append(rule.getMaxExpected()).append(",\n");
            sb.append("    \"deadline\": \"").append(escape(rule.getDeadline())).append("\",\n");
            sb.append("    \"warningMinutesBeforeDeadline\": ").append(rule.getWarningMinutesBeforeDeadline()).append(",\n");

            sb.append("    \"windows\": [");
            for (int w = 0; w < rule.getWindows().size(); w++) {
                MonitoringWindow window = rule.getWindows().get(w);
                if (w > 0) sb.append(", ");
                sb.append("{");
                sb.append("\"deadline\": \"").append(escape(window.getDeadline())).append("\", ");
                sb.append("\"minExpected\": ").append(window.getMinExpected()).append(", ");
                sb.append("\"maxExpected\": ").append(window.getMaxExpected());
                sb.append("}");
            }
            sb.append("],\n");

            sb.append("    \"weekdays\": \"").append(escape(rule.getWeekdays())).append("\",\n");
            sb.append("    \"monthDays\": \"").append(escape(rule.getMonthDays())).append("\",\n");
            sb.append("    \"specificDates\": \"").append(escape(rule.getSpecificDates())).append("\",\n");
            sb.append("    \"useHistoricalBaseline\": ").append(rule.isUseHistoricalBaseline()).append(",\n");
            sb.append("    \"historicalDays\": ").append(rule.getHistoricalDays()).append(",\n");
            sb.append("    \"minPercentOfAverage\": ").append(rule.getMinPercentOfAverage()).append("\n");
            sb.append("  }");
        }

        sb.append("\n]\n");
        FileJsonStore.writeFile(filePath, sb.toString());
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

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
