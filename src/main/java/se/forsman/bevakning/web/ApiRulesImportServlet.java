package se.forsman.bevakning.web;

import se.forsman.bevakning.admin.RuleImportExportService;
import se.forsman.bevakning.bootstrap.AppBootstrap;
import se.forsman.bevakning.config.AppConfig;
import se.forsman.bevakning.service.DashboardService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;

@WebServlet("/api/admin/rules/import")
public class ApiRulesImportServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RuleImportExportService service = new RuleImportExportService(new AppConfig());

        StringBuilder sb = new StringBuilder();
        BufferedReader reader = req.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }

        service.importRulesJson(sb.toString());

        DashboardService dashboardService =
                (DashboardService) getServletContext().getAttribute(AppBootstrap.DASHBOARD_SERVICE_KEY);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"message\":\"Regler importerade\"}");
    }
}
