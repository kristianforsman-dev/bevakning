package se.forsman.bevakning.domain;

public class MonitoringWindow {
    private final String deadline;
    private final int minExpected;
    private final int maxExpected;

    public MonitoringWindow(String deadline, int minExpected, int maxExpected) {
        this.deadline = deadline;
        this.minExpected = minExpected;
        this.maxExpected = maxExpected;
    }

    public String getDeadline() {
        return deadline;
    }

    public int getMinExpected() {
        return minExpected;
    }

    public int getMaxExpected() {
        return maxExpected;
    }
}
