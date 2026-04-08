package se.forsman.bevakning.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RefreshScheduler {
    private final CachedDashboardService cachedDashboardService;
    private final int refreshSeconds;
    private ScheduledExecutorService executorService;

    public RefreshScheduler(CachedDashboardService cachedDashboardService, int refreshSeconds) {
        this.cachedDashboardService = cachedDashboardService;
        this.refreshSeconds = refreshSeconds;
    }

    public void start() {
        if (executorService != null) {
            return;
        }
        executorService = Executors.newSingleThreadScheduledExecutor();
        cachedDashboardService.refresh();
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    cachedDashboardService.refresh();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, refreshSeconds, refreshSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
    }
}
