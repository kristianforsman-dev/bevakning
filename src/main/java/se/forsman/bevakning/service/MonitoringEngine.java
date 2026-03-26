package se.forsman.bevakning.service;

import se.forsman.bevakning.domain.AlertStatus;
import se.forsman.bevakning.domain.FlowEvent;
import se.forsman.bevakning.domain.FlowKey;
import se.forsman.bevakning.domain.MonitoringRule;
import se.forsman.bevakning.domain.MonitoringWindow;
import se.forsman.bevakning.domain.RuleEvaluation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class MonitoringEngine {

    public RuleEvaluation evaluate(MonitoringRule rule, Map<FlowKey, Integer> countsToday, List<FlowEvent> allEvents) {
        int countToday = getCountToday(rule, countsToday);
        LocalDateTime now = LocalDateTime.now();

        if ("MULTI_WINDOW".equalsIgnoreCase(rule.getScheduleType()) && !rule.getWindows().isEmpty()) {
            return evaluateMultiWindow(rule, countToday, now);
        }

        return evaluateSingleWindow(rule, countToday, now);
    }

    private RuleEvaluation evaluateSingleWindow(MonitoringRule rule, int countToday, LocalDateTime now) {
        LocalDateTime deadline = LocalDate.now().atTime(LocalTime.parse(rule.getDeadline()));
        AlertStatus status = evaluateAgainstThresholds(
                countToday,
                rule.getMinExpected(),
                deadline,
                rule.getWarningMinutesBeforeDeadline(),
                now
        );

        String message = buildMessage(status, countToday, rule.getMinExpected(), deadline.toLocalTime().toString());
        return new RuleEvaluation(rule, status, countToday, deadline.toLocalTime().toString(), message);
    }

    private RuleEvaluation evaluateMultiWindow(MonitoringRule rule, int countToday, LocalDateTime now) {
        AlertStatus highest = AlertStatus.OK;
        String activeDeadline = null;
        String message = "OK";

        int accumulatedMin = 0;
        for (MonitoringWindow window : rule.getWindows()) {
            accumulatedMin += window.getMinExpected();
            LocalDateTime deadline = LocalDate.now().atTime(LocalTime.parse(window.getDeadline()));
            AlertStatus status = evaluateAgainstThresholds(
                    countToday,
                    accumulatedMin,
                    deadline,
                    rule.getWarningMinutesBeforeDeadline(),
                    now
            );

            if (severity(status) > severity(highest)) {
                highest = status;
                activeDeadline = window.getDeadline();
                message = buildMessage(status, countToday, accumulatedMin, window.getDeadline());
            }
        }

        if (activeDeadline == null && !rule.getWindows().isEmpty()) {
            activeDeadline = rule.getWindows().get(rule.getWindows().size() - 1).getDeadline();
        }

        return new RuleEvaluation(rule, highest, countToday, activeDeadline, message);
    }

    private AlertStatus evaluateAgainstThresholds(int actualCount,
                                                  int minExpected,
                                                  LocalDateTime deadline,
                                                  int warningMinutesBeforeDeadline,
                                                  LocalDateTime now) {
        if (actualCount >= minExpected) {
            return AlertStatus.OK;
        }

        if (!now.isAfter(deadline)) {
            long minutesLeft = ChronoUnit.MINUTES.between(now, deadline);
            if (minutesLeft <= warningMinutesBeforeDeadline) {
                return AlertStatus.WARNING;
            }
            return AlertStatus.INFO;
        }

        return AlertStatus.ERROR;
    }

    private int getCountToday(MonitoringRule rule, Map<FlowKey, Integer> countsToday) {
        Integer count = countsToday.get(rule.toKey());
        return count == null ? 0 : count.intValue();
    }

    private int severity(AlertStatus status) {
        switch (status) {
            case ERROR: return 4;
            case WARNING: return 3;
            case INFO: return 2;
            default: return 1;
        }
    }

    private String buildMessage(AlertStatus status, int actual, int expected, String deadline) {
        if (status == AlertStatus.OK) {
            return "Tillräckligt antal inkommet";
        }
        if (status == AlertStatus.INFO) {
            return "Leverans pågår. " + actual + " av minst " + expected + " före " + deadline;
        }
        if (status == AlertStatus.WARNING) {
            return "Risk för underskott. " + actual + " av minst " + expected + " före " + deadline;
        }
        return "Deadline passerad. " + actual + " av minst " + expected + " före " + deadline;
    }
}
