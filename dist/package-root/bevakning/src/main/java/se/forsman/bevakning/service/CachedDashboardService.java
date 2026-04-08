package se.forsman.bevakning.service;

import se.forsman.bevakning.domain.DashboardSnapshot;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

public class CachedDashboardService {
    private final DashboardService delegate;
    private final AtomicReference<DashboardSnapshot> snapshotRef = new AtomicReference<DashboardSnapshot>();
    private final AtomicReference<LocalDateTime> refreshedAtRef = new AtomicReference<LocalDateTime>();

    public CachedDashboardService(DashboardService delegate) {
        this.delegate = delegate;
    }

    public synchronized DashboardSnapshot refresh() {
        DashboardSnapshot snapshot = delegate.getSnapshot();
        snapshotRef.set(snapshot);
        refreshedAtRef.set(LocalDateTime.now());
        return snapshot;
    }

    public DashboardSnapshot getSnapshot() {
        DashboardSnapshot snapshot = snapshotRef.get();
        if (snapshot == null) {
            return refresh();
        }
        return snapshot;
    }

    public LocalDateTime getRefreshedAt() {
        return refreshedAtRef.get();
    }
}
