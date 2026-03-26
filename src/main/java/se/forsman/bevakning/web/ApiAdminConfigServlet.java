package se.forsman.bevakning.web;

import se.forsman.bevakning.admin.AdminConfigService;
import se.forsman.bevakning.app.AppServices;
import se.forsman.bevakning.bootstrap.AppBootstrap;
import se.forsman.bevakning.config.AppConfig;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/admin/config")
public class ApiAdminConfigServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppServices services = (AppServices) getServletContext().getAttribute(AppBootstrap.APP_SERVICES_KEY);
        AdminConfigService adminConfigService = new AdminConfigService(new AppConfig(), services);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(adminConfigService.toJson());
    }
}
