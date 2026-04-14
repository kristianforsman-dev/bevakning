package se.forsman.bevakning.web;

import se.forsman.bevakning.bootstrap.AppBootstrap;
import se.forsman.bevakning.dev.DevJson;
import se.forsman.bevakning.domain.DashboardSnapshot;
import se.forsman.bevakning.service.CachedDashboardService;
import se.forsman.bevakning.service.DashboardService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet("/api/dashboard")
public class ApiDashboardServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json; charset=UTF-8");

        Object serviceObj = getServletContext().getAttribute(AppBootstrap.DASHBOARD_SERVICE_KEY);

        try {
            DashboardSnapshot snapshot;
            if (serviceObj instanceof CachedDashboardService) {
                CachedDashboardService service = (CachedDashboardService) serviceObj;
                snapshot = service.getSnapshot();
                resp.getWriter().write(DevJson.dashboard(snapshot, service.getRefreshedAt()));
                return;
            }

            if (serviceObj instanceof DashboardService) {
                DashboardService service = (DashboardService) serviceObj;
                snapshot = service.getSnapshot();
                resp.getWriter().write(DevJson.dashboard(snapshot));
                return;
            }

            resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            resp.getWriter().write("{\"message\":\"Dashboard service ej tillgänglig i webb-läge\"}");
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage().replace("\"", "\\\"");
            resp.getWriter().write("{\"message\":\"" + msg + "\"}");
        }
    }
}
