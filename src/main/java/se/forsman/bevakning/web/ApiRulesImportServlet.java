package se.forsman.bevakning.web;

import se.forsman.bevakning.admin.RuleImportExportService;
import se.forsman.bevakning.config.AppConfig;
import se.forsman.bevakning.dev.DevHttp;
import se.forsman.bevakning.web.json.JsonResponseUtils;
import se.forsman.bevakning.web.json.ServletJsonWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/admin/rules/import")
public class ApiRulesImportServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RuleImportExportService service = new RuleImportExportService(new AppConfig());

        StringBuilder sb = new StringBuilder();
        java.io.BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }

        service.importRulesJson(sb.toString());
        ServletJsonWriter.writeJson(resp, JsonResponseUtils.message("Regler importerade"));
    }
}
