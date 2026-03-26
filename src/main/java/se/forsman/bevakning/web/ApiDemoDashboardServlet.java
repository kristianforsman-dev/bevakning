package se.forsman.bevakning.web;

import se.forsman.bevakning.bootstrap.AppBootstrap;
import se.forsman.bevakning.domain.DashboardSnapshot;
import se.forsman.bevakning.domain.RuleEvaluation;
import se.forsman.bevakning.service.DashboardService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/api/demo-dashboard")
public class ApiDemoDashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        DashboardService dashboardService =
                (DashboardService) getServletContext().getAttribute(AppBootstrap.DASHBOARD_SERVICE_KEY);

        DashboardSnapshot snapshot = dashboardService.getSnapshot();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        PrintWriter out = resp.getWriter();
        out.write(toJson(snapshot));
        out.flush();
    }

    private String toJson(DashboardSnapshot snapshot) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"totalFlowsToday\":").append(snapshot.getTotalFlowsToday()).append(",");
        sb.append("\"ok\":").append(snapshot.getOk()).append(",");
        sb.append("\"info\":").append(snapshot.getInfo()).append(",");
        sb.append("\"warning\":").append(snapshot.getWarning()).append(",");
        sb.append("\"error\":").append(snapshot.getError()).append(",");
        sb.append("\"rows\":[");
        for (int i = 0; i < snapshot.getRows().size(); i++) {
            RuleEvaluation row = snapshot.getRows().get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{");
            sb.append("\"id\":\"").append(escape(row.getRule().getId())).append("\",");
            sb.append("\"sender\":\"").append(escape(row.getRule().getSender())).append("\",");
            sb.append("\"receiver\":\"").append(escape(row.getRule().getReceiver())).append("\",");
            sb.append("\"msgType\":\"").append(escape(row.getRule().getMsgType())).append("\",");
            sb.append("\"description\":\"").append(escape(row.getRule().getDescription())).append("\",");
            sb.append("\"status\":\"").append(row.getStatus().name()).append("\",");
            sb.append("\"countToday\":").append(row.getCountToday()).append(",");
            sb.append("\"deadline\":\"").append(escape(row.getCurrentDeadline())).append("\",");
            sb.append("\"message\":\"").append(escape(row.getMessage())).append("\"");
            sb.append("}");
        }
        sb.append("]");
        sb.append("}");
        return sb.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
