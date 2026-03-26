package se.forsman.bevakning.service;

import se.forsman.bevakning.domain.AlertStatus;
import se.forsman.bevakning.domain.BackendStatus;
import se.forsman.bevakning.domain.DailyFlowCount;
import se.forsman.bevakning.domain.DashboardSnapshot;
import se.forsman.bevakning.domain.FlowKey;
import se.forsman.bevakning.domain.MonitoringRule;
import se.forsman.bevakning.domain.RuleEvaluation;
import se.forsman.bevakning.repository.FlowRepository;
import se.forsman.bevakning.repository.MonitoringRuleRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardService {
    private final FlowRepository flowRepository;
    private final MonitoringRuleRepository monitoringRuleRepository;
    private final MonitoringEngine monitoringEngine;
    private final int historicalLookbackDays;

    public DashboardService(FlowRepository flowRepository,
                            MonitoringRuleRepository monitoringRuleRepository,
                            MonitoringEngine monitoringEngine,
                            int historicalLookbackDays) {
        this.flowRepository = flowRepository;
        this.monitoringRuleRepository = monitoringRuleRepository;
        this.monitoringEngine = monitoringEngine;
        this.historicalLookbackDays = historicalLookbackDays;
    }

    public DashboardSnapshot getSnapshot() {
        try {
            List<DailyFlowCount> todayFlowCounts = flowRepository.findTodayFlowCounts();
            List<DailyFlowCount> historicalFlowCounts = flowRepository.findHistoricalFlowCounts(historicalLookbackDays);

            List<DailyFlowCount> allDailyCounts = new ArrayList<DailyFlowCount>();
            allDailyCounts.addAll(todayFlowCounts);
            allDailyCounts.addAll(historicalFlowCounts);

            List<MonitoringRule> rules = monitoringRuleRepository.findAllRules();

            Map<FlowKey, Integer> countsToday = countToday(todayFlowCounts);
            int totalFlowsToday = totalFlowsToday(todayFlowCounts);

            List<RuleEvaluation> rows = new ArrayList<RuleEvaluation>();
            int ok = 0;
            int info = 0;
            int warning = 0;
            int error = 0;

            for (MonitoringRule rule : rules) {
                RuleEvaluation evaluation = monitoringEngine.evaluate(rule, countsToday, allDailyCounts);
                rows.add(evaluation);

                if (evaluation.getStatus() == AlertStatus.OK) ok++;
                else if (evaluation.getStatus() == AlertStatus.INFO) info++;
                else if (evaluation.getStatus() == AlertStatus.WARNING) warning++;
                else if (evaluation.getStatus() == AlertStatus.ERROR) error++;
            }

            BackendStatus backendStatus = flowRepository.getBackendStatus();

            return new DashboardSnapshot(
                    totalFlowsToday,
                    ok,
                    info,
                    warning,
                    error,
                    rows,
                    backendStatus.isOk(),
                    backendStatus.getMessage()
            );
        } catch (Exception e) {
            return new DashboardSnapshot(
                    0,
                    0,
                    0,
                    0,
                    0,
                    new ArrayList<RuleEvaluation>(),
                    false,
                    e.getMessage() == null ? "Tekniskt fel i backend" : e.getMessage()
            );
        }
    }

    private Map<FlowKey, Integer> countToday(List<DailyFlowCount> dailyFlowCounts) {
        Map<FlowKey, Integer> result = new HashMap<FlowKey, Integer>();

        for (DailyFlowCount daily : dailyFlowCounts) {
            result.put(daily.toKey(), daily.getCount());
        }

        return result;
    }

    private int totalFlowsToday(List<DailyFlowCount> dailyFlowCounts) {
        int count = 0;

        for (DailyFlowCount daily : dailyFlowCounts) {
            count += daily.getCount();
        }

        return count;
    }
}
