package se.forsman.bevakning.repository;

import se.forsman.bevakning.domain.AlertHistoryEntry;
import java.util.List;

public interface AlertHistoryRepository {
    List<AlertHistoryEntry> findAll();
    void save(AlertHistoryEntry entry);
}
