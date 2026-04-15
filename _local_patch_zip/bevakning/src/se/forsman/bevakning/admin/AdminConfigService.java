package se.forsman.bevakning.admin;

import se.forsman.bevakning.app.AppServices;
import se.forsman.bevakning.config.AppConfig;

public class AdminConfigService {
    private final AppConfig config;
    private final AppServices services;

    public AdminConfigService(AppConfig config, AppServices services) {
        this.config = config;
        this.services = services;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"appMode\":\"").append(escape(config.get("app.mode", "mock"))).append("\",");
        sb.append("\"refreshSeconds\":").append(config.getInt("db2.query.poll.seconds", config.getInt("app.refresh.seconds", 30))).append(","); sb.append("\"dashboardRefreshSeconds\":").append(config.getInt("app.refresh.dashboard.seconds", 5)).append(","); sb.append("\"historyRefreshSeconds\":").append(config.getInt("app.refresh.history.seconds", 10)).append(","); sb.append("\"flowStaleWarningMinutes\":").append(config.getInt("app.flow.stale.warning.minutes", 10)).append(","); sb.append("\"flowStaleErrorMinutes\":").append(config.getInt("app.flow.stale.error.minutes", 60)).append(",");
        sb.append("\"db2JndiName\":\"").append(escape(config.get("db2.jndi.name", ""))).append("\",");
        sb.append("\"db2QueryMode\":\"").append(escape(config.get("db2.query.mode", ""))).append("\",");
        sb.append("\"db2Schema\":\"").append(escape(config.get("db2.schema", ""))).append("\",");
        sb.append("\"db2LookbackDays\":").append(config.getInt("db2.lookback.days", 14)).append(",");
        sb.append("\"sourceDescription\":\"").append(escape(services.getSourceDescription())).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
