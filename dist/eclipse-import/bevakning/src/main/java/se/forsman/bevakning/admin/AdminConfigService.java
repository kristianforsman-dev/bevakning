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
        sb.append("\"refreshSeconds\":").append(config.getInt("app.refresh.seconds", 30)).append(",");
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
