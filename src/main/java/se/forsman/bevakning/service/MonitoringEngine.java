package se.forsman.bevakning.service;

import se.forsman.bevakning.domain.AlertStatus;
import se.forsman.bevakning.domain.DailyFlowCount;
import se.forsman.bevakning.domain.FlowKey;
import se.forsman.bevakning.domain.MonitoringRule;
import se.forsman.bevakning.domain.MonitoringWindow;
import se.forsman.bevakning.domain.RuleEvaluation;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MonitoringEngine {

    public RuleEvaluation evaluate(MonitoringRule rule,
                                   Map<FlowKey, Integer> countsToday,
                                   List<DailyFlowCount> dailyFlowCounts) {
        LocalDateTime now = LocalDateTime.now();
        int countToday = getCountToday(rule, countsToday);

        if (!isRuleApplicableToday(rule, now.toLocalDate())) {
            return new RuleEvaluation(rule, AlertStatus.OK, countToday, rule.getDeadline(), "Inte schemalagd idag");
        }

        if ("MULTI_WINDOW".equalsIgnoreCase(rule.getScheduleType()) && !rule.getWindows().isEmpty()) {
            return evaluateMultiWindow(rule, countToday, now);
        }

        return evaluateSingleWindow(rule, countToday, now, dailyFlowCounts);
    }

    private RuleEvaluation evaluateSingleWindow(MonitoringRule rule,
                                                int countToday,
                                                LocalDateTime now,
                                                List<DailyFlowCount> dailyFlowCounts) {
        LocalDateTime deadline = LocalDate.now().atTime(LocalTime.parse(rule.getDeadline()));
        int effectiveMinExpected = resolveEffectiveMinExpected(rule, dailyFlowCounts);

        AlertStatus status = evaluateAgainstThresholds(
                countToday,
                effectiveMinExpected,
                deadline,
                rule.getWarningMinutesBeforeDeadline(),
                now
        );

        String message = buildMessage(status, countToday, effectiveMinExpected, deadline.toLocalTime().toString(), rule);
        return new RuleEvaluation(rule, status, countToday, deadline.toLocalTime().toString(), message);
    }

    private RuleEvaluation evaluateMultiWindow(MonitoringRule rule, int countToday, LocalDateTime now) {
        AlertStatus highest = AlertStatus.OK;
        String activeDeadline = null;
        String message = "OK";

        int accumulatedMin = 0;
        for (MonitoringWindow window : rule.getWindows()) {
            accumulatedMin = Math.max(accumulatedMin, window.getMinExpected());
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
                message = buildMessage(status, countToday, accumulatedMin, window.getDeadline(), rule);
            }
        }

        if (activeDeadline == null && !rule.getWindows().isEmpty()) {
            activeDeadline = rule.getWindows().get(rule.getWindows().size() - 1).getDeadline();
        }

        return new RuleEvaluation(rule, highest, countToday, activeDeadline, message);
    }

    private int resolveEffectiveMinExpected(MonitoringRule rule, List<DailyFlowCount> dailyFlowCounts) {
        int base = rule.getMinExpected();
        if (!rule.isUseHistoricalBaseline()) {
            return base;
        }

        int avg = calculateHistoricalAverage(rule, dailyFlowCounts, rule.getHistoricalDays());
        if (avg <= 0) {
            return base;
        }

        int historicalMin = (int) Math.ceil(avg * (rule.getMinPercentOfAverage() / 100.0d));
        return Math.max(base, historicalMin);
    }

    private int calculateHistoricalAverage(MonitoringRule rule, List<DailyFlowCount> dailyFlowCounts, int historicalDays) {
        if (historicalDays <= 0) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        int total = 0;
        int includedDays = 0;

        for (int i = 1; i <= historicalDays; i++) {
            LocalDate day = today.minusDays(i);
            if (!isRuleApplicableToday(rule, day)) {
                continue;
            }

            int count = 0;
            for (DailyFlowCount daily : dailyFlowCounts) {
                if (!day.equals(daily.getFlowDate())) {
                    continue;
                }
                if (!sameFlow(rule, daily)) {
                    continue;
                }
                count += daily.getCount();
            }

            total += count;
            includedDays++;
        }

        if (includedDays == 0) {
            return 0;
        }
        return total / includedDays;
    }

    private boolean sameFlow(MonitoringRule rule, DailyFlowCount daily) {
        return safeEquals(rule.getSender(), daily.getSender())
                && safeEquals(rule.getReceiver(), daily.getReceiver())
                && safeEquals(rule.getMsgType(), daily.getMsgType());
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private boolean isRuleApplicableToday(MonitoringRule rule, LocalDate date) {
        if (!rule.isActive()) {
            return false;
        }

        if (!matchesSpecificDates(rule.getSpecificDates(), date)) {
            return false;
        }

        if (!matchesWeekdays(rule.getWeekdays(), date.getDayOfWeek())) {
            return false;
        }

        if (!matchesMonthDays(rule.getMonthDays(), date.getDayOfMonth())) {
            return false;
        }

        return true;
    }

    private boolean matchesSpecificDates(String csv, LocalDate date) {
        Set<String> values = csvSet(csv);
        if (values.isEmpty()) {
            return true;
        }
        return values.contains(date.toString());
    }

    private boolean matchesWeekdays(String csv, DayOfWeek dayOfWeek) {
        Set<String> values = csvSet(csv);
        if (values.isEmpty()) {
            return true;
        }

        String full = dayOfWeek.name();
        String shortCode = full.substring(0, 3);

        if (values.contains(full) || values.contains(shortCode)) {
            return true;
        }

        switch (dayOfWeek) {
            case MONDAY: return values.contains("MAN");
            case TUESDAY: return values.contains("TIS");
            case WEDNESDAY: return values.contains("ONS");
            case THURSDAY: return values.contains("TOR");
            case FRIDAY: return values.contains("FRE");
            case SATURDAY: return values.contains("LOR");
            case SUNDAY: return values.contains("SON");
            default: return false;
        }
    }

    private boolean matchesMonthDays(String csv, int dayOfMonth) {
        Set<String> values = csvSet(csv);
        if (values.isEmpty()) {
            return true;
        }
        return values.contains(String.valueOf(dayOfMonth));
    }

    private Set<String> csvSet(String csv) {
        Set<String> result = new HashSet<String>();
        if (csv == null || csv.trim().isEmpty()) {
            return result;
        }
        String[] parts = csv.split(",");
        for (String part : parts) {
            String trimmed = part == null ? "" : part.trim().toUpperCase();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
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

    private String buildMessage(AlertStatus status, int actual, int expected, String deadline, MonitoringRule rule) {
        String baseline = rule.isUseHistoricalBaseline()
                ? " (inkl historik " + rule.getHistoricalDays() + "d/" + rule.getMinPercentOfAverage() + "%)"
                : "";

        if (status == AlertStatus.OK) {
            return "Tillräckligt antal inkommet" + baseline;
        }
        if (status == AlertStatus.INFO) {
            return "Leverans pågår. " + actual + " av minst " + expected + " före " + deadline + baseline;
        }
        if (status == AlertStatus.WARNING) {
            return "Risk för underskott. " + actual + " av minst " + expected + " före " + deadline + baseline;
        }
        return "Deadline passerad. " + actual + " av minst " + expected + " före " + deadline + baseline;
    }
}
