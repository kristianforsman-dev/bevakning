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
import java.util.Set;

public class MonitoringEngine {

    public RuleEvaluation evaluate(MonitoringRule rule,
                                   java.util.Map<FlowKey, Integer> countsToday,
                                   List<DailyFlowCount> dailyFlowCounts) {
        LocalDateTime now = LocalDateTime.now();
        int countToday = getCountToday(rule, countsToday);

        if (!isRuleApplicableToday(rule, now.toLocalDate())) {
            return new RuleEvaluation(
                    rule,
                    AlertStatus.OK,
                    countToday,
                    rule.getDeadline(),
                    "Regeln gäller inte idag",
                    buildOccurrenceKey(rule, now.toLocalDate(), rule.getDeadline()),
                    false,
                    "",
                    ""
            );
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

        String message = buildSingleWindowMessage(
                status,
                countToday,
                effectiveMinExpected,
                deadline
        );

        return new RuleEvaluation(
                rule,
                status,
                countToday,
                deadline.toLocalTime().toString(),
                message,
                buildOccurrenceKey(rule, deadline.toLocalDate(), deadline.toLocalTime().toString()),
                false,
                "",
                ""
        );
    }

    private RuleEvaluation evaluateMultiWindow(MonitoringRule rule, int countToday, LocalDateTime now) {
        AlertStatus highest = AlertStatus.OK;
        String activeDeadline = rule.getDeadline();
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
                message = buildMultiWindowMessage(status, countToday, accumulatedMin, deadline);
            }
        }

        return new RuleEvaluation(
                rule,
                highest,
                countToday,
                activeDeadline,
                message,
                buildOccurrenceKey(rule, LocalDate.now(), activeDeadline),
                false,
                "",
                ""
        );
    }

    private String buildOccurrenceKey(MonitoringRule rule, LocalDate date, String deadline) {
        return rule.getId() + "|" + date.toString() + "|" + (deadline == null ? "" : deadline);
    }

    private int resolveEffectiveMinExpected(MonitoringRule rule, List<DailyFlowCount> dailyFlowCounts) {
        int configuredMin = rule.getMinExpected();

        if (!rule.isUseHistoricalBaseline()) {
            return configuredMin;
        }

        int historicalAverage = calculateHistoricalAverage(rule, dailyFlowCounts, rule.getHistoricalDays());
        if (historicalAverage <= 0) {
            return configuredMin;
        }

        int historicalMin = (int) Math.ceil(historicalAverage * (rule.getMinPercentOfAverage() / 100.0d));
        return Math.max(configuredMin, historicalMin);
    }

    private int calculateHistoricalAverage(MonitoringRule rule,
                                           List<DailyFlowCount> dailyFlowCounts,
                                           int historicalDays) {
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
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }

    private boolean isRuleApplicableToday(MonitoringRule rule, LocalDate date) {
        if (!rule.isActive()) {
            return false;
        }

        boolean hasSpecificDates = !csvSet(rule.getSpecificDates()).isEmpty();
        boolean hasWeekdays = !csvSet(rule.getWeekdays()).isEmpty();
        boolean hasMonthDays = !csvSet(rule.getMonthDays()).isEmpty();

        boolean hasAnyScheduleRestriction = hasSpecificDates || hasWeekdays || hasMonthDays;

        if (!hasAnyScheduleRestriction) {
            return true;
        }

        boolean specificDateMatch = hasSpecificDates && matchesSpecificDates(rule.getSpecificDates(), date);
        boolean weekdayMatch = hasWeekdays && matchesWeekdays(rule.getWeekdays(), date.getDayOfWeek());
        boolean monthDayMatch = hasMonthDays && matchesMonthDays(rule.getMonthDays(), date.getDayOfMonth());

        return specificDateMatch || weekdayMatch || monthDayMatch;
    }

    private boolean matchesSpecificDates(String csv, LocalDate date) {
        Set<String> values = csvSet(csv);
        if (values.isEmpty()) {
            return false;
        }
        return values.contains(date.toString().toUpperCase());
    }

    private boolean matchesWeekdays(String csv, DayOfWeek dayOfWeek) {
        Set<String> values = csvSet(csv);
        if (values.isEmpty()) {
            return false;
        }

        String full = dayOfWeek.name();
        String shortCode = full.substring(0, 3);

        if (values.contains(full) || values.contains(shortCode)) {
            return true;
        }

        switch (dayOfWeek) {
            case MONDAY:
                return values.contains("MON") || values.contains("MAN");
            case TUESDAY:
                return values.contains("TUE") || values.contains("TIS");
            case WEDNESDAY:
                return values.contains("WED") || values.contains("ONS");
            case THURSDAY:
                return values.contains("THU") || values.contains("TOR");
            case FRIDAY:
                return values.contains("FRI") || values.contains("FRE");
            case SATURDAY:
                return values.contains("SAT") || values.contains("LOR");
            case SUNDAY:
                return values.contains("SUN") || values.contains("SON");
            default:
                return false;
        }
    }

    private boolean matchesMonthDays(String csv, int dayOfMonth) {
        Set<String> values = csvSet(csv);
        if (values.isEmpty()) {
            return false;
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

    private String buildSingleWindowMessage(AlertStatus status,
                                            int actualCount,
                                            int effectiveMinExpected,
                                            LocalDateTime deadline) {
        String deadlineText = deadline.toLocalTime().toString();

        if (status == AlertStatus.OK) {
            return "OK: " + actualCount + " av minst " + effectiveMinExpected + " mottagna före " + deadlineText;
        }

        if (status == AlertStatus.INFO) {
            return "Pågår: " + actualCount + " av minst " + effectiveMinExpected + " mottagna, deadline " + deadlineText;
        }

        if (status == AlertStatus.WARNING) {
            return "Varning: " + actualCount + " av minst " + effectiveMinExpected + " mottagna, nära deadline " + deadlineText;
        }

        return "Fel: " + actualCount + " av minst " + effectiveMinExpected + " mottagna efter deadline " + deadlineText;
    }

    private String buildMultiWindowMessage(AlertStatus status,
                                           int actualCount,
                                           int minExpected,
                                           LocalDateTime deadline) {
        String deadlineText = deadline.toLocalTime().toString();

        if (status == AlertStatus.OK) {
            return "OK: " + actualCount + " av minst " + minExpected + " mottagna före " + deadlineText;
        }

        if (status == AlertStatus.INFO) {
            return "Pågår: " + actualCount + " av minst " + minExpected + " mottagna, deadline " + deadlineText;
        }

        if (status == AlertStatus.WARNING) {
            return "Varning: " + actualCount + " av minst " + minExpected + " mottagna, nära deadline " + deadlineText;
        }

        return "Fel: " + actualCount + " av minst " + minExpected + " mottagna efter deadline " + deadlineText;
    }

    private int getCountToday(MonitoringRule rule, java.util.Map<FlowKey, Integer> countsToday) {
        Integer count = countsToday.get(rule.toKey());
        return count == null ? 0 : count.intValue();
    }

    private int severity(AlertStatus status) {
        switch (status) {
            case ERROR:
                return 4;
            case WARNING:
                return 3;
            case INFO:
                return 2;
            default:
                return 1;
        }
    }
}
