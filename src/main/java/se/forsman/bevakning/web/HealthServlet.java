package se.forsman.bevakning.web;

import se.forsman.bevakning.app.AppServices;
import se.forsman.bevakning.bootstrap.AppBootstrap;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/health")
public class HealthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppServices services = (AppServices) getServletContext().getAttribute(AppBootstrap.APP_SERVICES_KEY);

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(
                "{"
                        + "\"status\":\"UP\","
                        + "\"mode\":\"" + escape(services.getMode()) + "\","
                        + "\"source\":\"" + escape(services.getSourceDescription()) + "\""
                        + "}"
        );
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
