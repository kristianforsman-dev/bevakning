package se.forsman.bevakning.web;

import se.forsman.bevakning.app.AppServices;
import se.forsman.bevakning.bootstrap.AppBootstrap;
import se.forsman.bevakning.web.json.JsonResponseUtils;
import se.forsman.bevakning.web.json.ServletJsonWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/health")
public class HealthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AppServices services = (AppServices) getServletContext().getAttribute(AppBootstrap.APP_SERVICES_KEY);
        ServletJsonWriter.writeJson(
                resp,
                JsonResponseUtils.health("UP", services.getMode(), services.getSourceDescription())
        );
    }
}
