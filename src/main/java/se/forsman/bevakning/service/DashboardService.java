package se.forsman.bevakning.service;

import se.forsman.bevakning.domain.AlertStatus;
import se.forsman.bevakning.domain.DashboardSnapshot;
import se.forsman.bevakning.domain.FlowEvent;
import se.forsman.bevakning.domain.FlowKey;
import se.forsman.bevakning.domain.MonitoringRule;
import se.forsman.bevakning.domain.RuleEvaluation;
import se.forsman.bevakning.repository.FlowRepository;
import se.forsman.bevakning.repository.MonitoringRuleRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardService {
    private final FlowRepository flowRepository;
    private final MonitoringRuleRepository monitoringRuleRepository;
    private final MonitoringEngine monitoringEngine;

    public DashboardService(FlowRepository flowRepository,
                            MonitoringRuleRepository monitoringRuleRepository,
                            MonitoringEngine monitoringEngine) {
        this.flowRepository = flowRepository;
        this.monitoringRuleRepository = monitoringRuleRepository;
        this.monitoringEngine = monitoringEngine;
    }

    public DashboardSnapshot getSnapshot() {
        List<FlowEvent> allEvents = flowRepository.findAllFlowEvents();
        List<MonitoringRule> rules = monitoringRuleRepository.findAllRules();

        Map<FlowKey, Integer> countsToday = countToday(allEvents);
        int totalFlowsToday = totalFlowsToday(allEvents);

        List<RuleEvaluation> rows = new ArrayList<RuleEvaluation>();
        int ok = 0;
        int info = 0;
        int warning = 0;
        int error = 0;

        for (MonitoringRule rule : rules) {
            if (!rule.isActive()) {
                continue;
            }

            RuleEvaluation evaluation = monitoringEngine.evaluate(rule, countsToday, allEvents);
            rows.add(evaluation);

            if (evaluation.getStatus() == AlertStatus.OK) ok++;
            else if (evaluation.getStatus() == AlertStatus.INFO) info++;
            else if (evaluation.getStatus() == AlertStatus.WARNING) warning++;
            else if (evaluation.getStatus() == AlertStatus.ERROR) error++;
        }

        return new DashboardSnapshot(totalFlowsToday, ok, info, warning, error, rows);
    }

    private Map<FlowKey, Integer> countToday(List<FlowEvent> events) {
        Map<FlowKey, Integer> result = new HashMap<FlowKey, Integer>();
        LocalDate today = LocalDate.now();

        for (FlowEvent event : events) {
            if (event.getStarted() == null) {
                continue;
            }
            if (!today.equals(event.getStarted().toLocalDate())) {
                continue;
            }
            FlowKey key = event.toKey();
            Integer current = result.get(key);
            result.put(key, current == null ? 1 : current + 1);
        }

        return result;
    }

    private int totalFlowsToday(List<FlowEvent> events) {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (FlowEvent event : events) {
            if (event.getStarted() != null && today.equals(event.getStarted().toLocalDate())) {
                count++;
            }
        }
        return count;
    }
}
