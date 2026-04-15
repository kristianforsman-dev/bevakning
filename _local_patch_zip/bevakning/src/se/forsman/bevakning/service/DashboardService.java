package se.forsman.bevakning.service;

import se.forsman.bevakning.domain.Acknowledgement;
import se.forsman.bevakning.domain.AlertHistoryEntry;
import se.forsman.bevakning.domain.AlertStatus;
import se.forsman.bevakning.domain.BackendStatus;
import se.forsman.bevakning.domain.DailyFlowCount;
import se.forsman.bevakning.domain.DashboardSnapshot;
import se.forsman.bevakning.domain.FlowKey;
import se.forsman.bevakning.domain.MonitoringRule;
import se.forsman.bevakning.domain.RuleEvaluation;
import se.forsman.bevakning.repository.AcknowledgementRepository;
import se.forsman.bevakning.repository.AlertHistoryRepository;
import se.forsman.bevakning.repository.FlowRepository;
import se.forsman.bevakning.repository.MonitoringRuleRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DashboardService {
    private final FlowRepository flowRepository;
    private final MonitoringRuleRepository monitoringRuleRepository;
    private final AlertHistoryRepository alertHistoryRepository;
    private final AcknowledgementRepository acknowledgementRepository;
    private final MonitoringEngine monitoringEngine;
    private final int historicalLookbackDays;

    public DashboardService(FlowRepository flowRepository,
                            MonitoringRuleRepository monitoringRuleRepository,
                            AlertHistoryRepository alertHistoryRepository,
                            AcknowledgementRepository acknowledgementRepository,
                            MonitoringEngine monitoringEngine,
                            int historicalLookbackDays) {
        this.flowRepository = flowRepository;
        this.monitoringRuleRepository = monitoringRuleRepository;
        this.alertHistoryRepository = alertHistoryRepository;
        this.acknowledgementRepository = acknowledgementRepository;
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
            List<AlertHistoryEntry> historyEntries = alertHistoryRepository.findAll();
            List<Acknowledgement> acknowledgements = acknowledgementRepository.findAll();

            Map<String, AlertHistoryEntry> latestHistoryByRule = latestHistoryByRule(historyEntries);
            Map<FlowKey, Integer> countsToday = countToday(todayFlowCounts);
            int totalFlowsToday = totalFlowsToday(todayFlowCounts);

            List<RuleEvaluation> rows = new ArrayList<RuleEvaluation>();
            int ok = 0;
            int info = 0;
            int warning = 0;
            int error = 0;

            for (MonitoringRule rule : rules) {
                RuleEvaluation rawEvaluation = monitoringEngine.evaluate(rule, countsToday, allDailyCounts);

                recordStatusTransitionIfNeeded(rule, rawEvaluation, latestHistoryByRule);

                Acknowledgement activeAck = findLatestMatchingAcknowledgement(
                        acknowledgements,
                        rule.getId(),
                        rawEvaluation.getOccurrenceKey(),
                        rawEvaluation.getStatus().name()
                );

                RuleEvaluation evaluation = new RuleEvaluation(
                        rawEvaluation.getRule(),
                        rawEvaluation.getStatus(),
                        rawEvaluation.getCountToday(),
                        rawEvaluation.getCurrentDeadline(),
                        rawEvaluation.getMessage(),
                        rawEvaluation.getOccurrenceKey(),
                        activeAck != null,
                        activeAck == null ? "" : activeAck.getAcknowledgedBy(),
                        activeAck == null ? "" : activeAck.getAcknowledgedAt()
                );

                rows.add(evaluation);

                if (evaluation.getStatus() == AlertStatus.OK) ok++;
                else if (evaluation.getStatus() == AlertStatus.INFO) info++;
                else if (evaluation.getStatus() == AlertStatus.WARNING) warning++;
                else if (evaluation.getStatus() == AlertStatus.ERROR) error++;
            }

            BackendStatus backendStatus = flowRepository.getBackendStatus();
            String incomingStartedAt = formatDateTime(flowRepository.findLatestIncomingStartedAt());
            String outgoingStartedAt = formatDateTime(flowRepository.findLatestOutgoingStartedAt());

            return new DashboardSnapshot(totalFlowsToday, ok, info, warning, error, rows, backendStatus.isOk(), backendStatus.getMessage(), incomingStartedAt, outgoingStartedAt);
        } catch (Exception e) {
            return new DashboardSnapshot(
                    0,
                    0,
                    0,
                    0,
                    0,
                    new ArrayList<RuleEvaluation>(),
                    false,
                    e.getMessage() == null ? "Tekniskt fel i backend" : e.getMessage(),
                    null,
                    null
            );
        }
    }

    private void recordStatusTransitionIfNeeded(MonitoringRule rule,
                                                RuleEvaluation evaluation,
                                                Map<String, AlertHistoryEntry> latestHistoryByRule) {
        String ruleId = rule.getId();
        String currentStatus = evaluation.getStatus().name();
        String currentOccurrenceKey = safe(evaluation.getOccurrenceKey());
        AlertHistoryEntry latest = latestHistoryByRule.get(ruleId);

        if (latest == null) {
            AlertHistoryEntry entry = new AlertHistoryEntry(
                    "hist-" + UUID.randomUUID().toString(),
                    ruleId,
                    currentOccurrenceKey,
                    "STATUS_DETECTED",
                    currentStatus,
                    "Status upptäckt: " + currentStatus + ". " + safe(evaluation.getMessage()),
                    LocalDateTime.now().toString(),
                    "system"
            );
            alertHistoryRepository.save(entry);
            latestHistoryByRule.put(ruleId, entry);
            return;
        }

        String latestStatus = safe(latest.getStatus());
        String latestOccurrenceKey = safe(latest.getOccurrenceKey());

        if (latestStatus.equalsIgnoreCase(currentStatus)
                && latestOccurrenceKey.equals(currentOccurrenceKey)) {
            return;
        }

        String eventType;
        String message;

        if (!latestOccurrenceKey.equals(currentOccurrenceKey)) {
            eventType = "OCCURRENCE_STARTED";
            message = "Ny bevakningsinstans: " + currentOccurrenceKey + ". Status: " + currentStatus + ". " + safe(evaluation.getMessage());
        } else {
            eventType = "STATUS_CHANGED";
            message = "Status ändrad: " + latestStatus + " -> " + currentStatus + ". " + safe(evaluation.getMessage());
        }

        AlertHistoryEntry entry = new AlertHistoryEntry(
                "hist-" + UUID.randomUUID().toString(),
                ruleId,
                currentOccurrenceKey,
                eventType,
                currentStatus,
                message,
                LocalDateTime.now().toString(),
                "system"
        );

        alertHistoryRepository.save(entry);
        latestHistoryByRule.put(ruleId, entry);
    }

    private Map<String, AlertHistoryEntry> latestHistoryByRule(List<AlertHistoryEntry> entries) {
        Map<String, AlertHistoryEntry> result = new HashMap<String, AlertHistoryEntry>();

        for (AlertHistoryEntry entry : entries) {
            if (entry == null || entry.getRuleId() == null || entry.getRuleId().trim().isEmpty()) {
                continue;
            }

            AlertHistoryEntry current = result.get(entry.getRuleId());
            if (current == null || compareCreatedAt(entry, current) > 0) {
                result.put(entry.getRuleId(), entry);
            }
        }

        return result;
    }

    private Acknowledgement findLatestMatchingAcknowledgement(List<Acknowledgement> all,
                                                              String ruleId,
                                                              String occurrenceKey,
                                                              String status) {
        Acknowledgement best = null;

        for (Acknowledgement ack : all) {
            if (ack == null) continue;
            if (!safe(ruleId).equals(safe(ack.getRuleId()))) continue;
            if (!safe(occurrenceKey).equals(safe(ack.getOccurrenceKey()))) continue;
            if (!safe(status).equalsIgnoreCase(safe(ack.getStatus()))) continue;

            if (best == null || safe(ack.getAcknowledgedAt()).compareTo(safe(best.getAcknowledgedAt())) > 0) {
                best = ack;
            }
        }

        return best;
    }

    private int compareCreatedAt(AlertHistoryEntry a, AlertHistoryEntry b) {
        return safe(a.getCreatedAt()).compareTo(safe(b.getCreatedAt()));
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
    }    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.plusHours(2).toString();
    }



    private String safe(String value) {
        return value == null ? "" : value;
    }
}
