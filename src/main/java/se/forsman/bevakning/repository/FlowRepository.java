package se.forsman.bevakning.repository;

import se.forsman.bevakning.domain.BackendStatus;
import se.forsman.bevakning.domain.DailyFlowCount;
import se.forsman.bevakning.domain.FlowEvent;
import se.forsman.bevakning.domain.FlowKey;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface FlowRepository {
    List<FlowEvent> findAllFlowEvents();

    default List<DailyFlowCount> findTodayFlowCounts() {
        return filterDailyCounts(findDailyFlowCounts(1), LocalDate.now(), LocalDate.now());
    }

    default List<DailyFlowCount> findHistoricalFlowCounts(int lookbackDays) {
        LocalDate fromDate = LocalDate.now().minusDays(lookbackDays);
        LocalDate toDate = LocalDate.now().minusDays(1);
        return filterDailyCounts(findDailyFlowCounts(lookbackDays), fromDate, toDate);
    }

    default BackendStatus getBackendStatus() {
        return new BackendStatus(true, "OK");
    }

    default List<DailyFlowCount> findDailyFlowCounts(int lookbackDays) {
        List<FlowEvent> events = findAllFlowEvents();
        Map<String, Integer> aggregated = new HashMap<String, Integer>();

        LocalDate fromDate = LocalDate.now().minusDays(lookbackDays);

        for (FlowEvent event : events) {
            if (event.getStarted() == null) {
                continue;
            }

            LocalDate day = event.getStarted().toLocalDate();
            if (day.isBefore(fromDate)) {
                continue;
            }

            FlowKey key = event.toKey();
            String aggregateKey = day.toString() + "|" + safe(key.getSender()) + "|" + safe(key.getReceiver()) + "|" + safe(key.getMsgType());
            Integer current = aggregated.get(aggregateKey);
            aggregated.put(aggregateKey, current == null ? 1 : current + 1);
        }

        List<DailyFlowCount> result = new ArrayList<DailyFlowCount>();
        for (Map.Entry<String, Integer> entry : aggregated.entrySet()) {
            String[] parts = entry.getKey().split("\\|", -1);
            result.add(new DailyFlowCount(
                    LocalDate.parse(parts[0]),
                    emptyToNull(parts[1]),
                    emptyToNull(parts[2]),
                    emptyToNull(parts[3]),
                    entry.getValue()
            ));
        }

        return result;
    }

    default List<DailyFlowCount> filterDailyCounts(List<DailyFlowCount> source, LocalDate from, LocalDate to) {
        List<DailyFlowCount> result = new ArrayList<DailyFlowCount>();
        for (DailyFlowCount row : source) {
            if (row.getFlowDate() == null) continue;
            if (row.getFlowDate().isBefore(from)) continue;
            if (row.getFlowDate().isAfter(to)) continue;
            result.add(row);
        }
        return result;
    }

    default String safe(String value) {
        return value == null ? "" : value;
    }

    default String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
