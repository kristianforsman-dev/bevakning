package se.forsman.bevakning.web;

import se.forsman.bevakning.admin.RuleImportExportService;
import se.forsman.bevakning.config.AppConfig;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/admin/rules/export")
public class ApiRulesExportServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RuleImportExportService service = new RuleImportExportService(new AppConfig());

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(service.exportRulesJson());
    }
}
