package se.forsman.bevakning.admin;

import se.forsman.bevakning.config.AppConfig;
import se.forsman.bevakning.util.FileJsonStore;

public class RuleImportExportService {
    private final String rulesPath;

    public RuleImportExportService(AppConfig config) {
        this.rulesPath = config.get("app.data.rules.path");
    }

    public String exportRulesJson() {
        return FileJsonStore.readFile(rulesPath);
    }

    public void importRulesJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("Importfilen är tom");
        }
        FileJsonStore.writeFile(rulesPath, json);
    }

    public String getRulesPath() {
        return rulesPath;
    }
}
