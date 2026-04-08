package se.forsman.bevakning.service;

import se.forsman.bevakning.domain.MonitoringRule;
import se.forsman.bevakning.domain.MonitoringWindow;
import se.forsman.bevakning.repository.EditableMonitoringRuleRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RuleService {
    private final EditableMonitoringRuleRepository repository;

    public RuleService(EditableMonitoringRuleRepository repository) {
        this.repository = repository;
    }

    public List<MonitoringRule> findAll() {
        return repository.findAllRules();
    }

    public MonitoringRule findById(String id) {
        return repository.findById(id);
    }

    public MonitoringRule saveOrUpdateRule(String id,
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
                                           String windowsSpec,
                                           String weekdays,
                                           String monthDays,
                                           String specificDates,
                                           boolean useHistoricalBaseline,
                                           int historicalDays,
                                           int minPercentOfAverage) {

        MonitoringRule rule = new MonitoringRule(
                empty(id) ? "rule-" + UUID.randomUUID().toString() : id,
                trim(sender),
                trim(receiver),
                trim(msgType),
                trim(description),
                defaultValue(trim(scheduleType), "DAILY"),
                active,
                minExpected,
                maxExpected,
                defaultValue(trim(deadline), "15:00"),
                warningMinutesBeforeDeadline,
                parseWindows(windowsSpec),
                trim(weekdays),
                trim(monthDays),
                trim(specificDates),
                useHistoricalBaseline,
                historicalDays,
                minPercentOfAverage
        );

        if (empty(id)) repository.save(rule);
        else repository.update(rule);

        return rule;
    }

    public void delete(String id) {
        repository.delete(id);
    }

    private List<MonitoringWindow> parseWindows(String windowsSpec) {
        List<MonitoringWindow> result = new ArrayList<MonitoringWindow>();
        if (windowsSpec == null || windowsSpec.trim().isEmpty()) {
            return result;
        }

        String[] rows = windowsSpec.split("\\r?\\n|;");
        for (String row : rows) {
            String line = row == null ? "" : row.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\|");
            if (parts.length < 3) {
                throw new IllegalArgumentException("Ogiltigt windowsSpec. Använd formatet HH:mm|min|max");
            }

            result.add(new MonitoringWindow(
                    parts[0].trim(),
                    Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim())
            ));
        }
        return result;
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean empty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String defaultValue(String value, String fallback) {
        return empty(value) ? fallback : value;
    }
}
