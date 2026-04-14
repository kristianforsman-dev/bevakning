package se.forsman.bevakning.web;

import se.forsman.bevakning.bootstrap.AppBootstrap;
import se.forsman.bevakning.domain.MonitoringRule;
import se.forsman.bevakning.domain.MonitoringWindow;
import se.forsman.bevakning.service.RuleService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@WebServlet("/api/rules")
public class ApiRulesServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RuleService service = (RuleService) getServletContext().getAttribute(AppBootstrap.RULE_SERVICE_KEY);
        List<MonitoringRule> rules = service.findAll();

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter out = resp.getWriter();

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < rules.size(); i++) {
            MonitoringRule r = rules.get(i);
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"id\":\"").append(escape(r.getId())).append("\",");
            sb.append("\"sender\":\"").append(escape(r.getSender())).append("\",");
            sb.append("\"receiver\":\"").append(escape(r.getReceiver())).append("\",");
            sb.append("\"msgType\":\"").append(escape(r.getMsgType())).append("\",");
            sb.append("\"description\":\"").append(escape(r.getDescription())).append("\",");
            sb.append("\"scheduleType\":\"").append(escape(r.getScheduleType())).append("\",");
            sb.append("\"active\":").append(r.isActive()).append(",");
            sb.append("\"minExpected\":").append(r.getMinExpected()).append(",");
            sb.append("\"maxExpected\":").append(r.getMaxExpected()).append(",");
            sb.append("\"deadline\":\"").append(escape(r.getDeadline())).append("\",");
            sb.append("\"warningMinutesBeforeDeadline\":").append(r.getWarningMinutesBeforeDeadline()).append(",");
            sb.append("\"weekdays\":\"").append(escape(r.getWeekdays())).append("\",");
            sb.append("\"monthDays\":\"").append(escape(r.getMonthDays())).append("\",");
            sb.append("\"specificDates\":\"").append(escape(r.getSpecificDates())).append("\",");
            sb.append("\"useHistoricalBaseline\":").append(r.isUseHistoricalBaseline()).append(",");
            sb.append("\"historicalDays\":").append(r.getHistoricalDays()).append(",");
            sb.append("\"minPercentOfAverage\":").append(r.getMinPercentOfAverage()).append(",");
            sb.append("\"windowsSpec\":\"").append(escape(toWindowsSpec(r.getWindows()))).append("\"");
            sb.append("}");
        }
        sb.append("]");
        out.write(sb.toString());
        out.flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RuleService service = (RuleService) getServletContext().getAttribute(AppBootstrap.RULE_SERVICE_KEY);
        service.saveOrUpdateRule(
                req.getParameter("id"),
                req.getParameter("sender"),
                req.getParameter("receiver"),
                req.getParameter("msgType"),
                req.getParameter("description"),
                req.getParameter("scheduleType"),
                toBooleanDefaultTrue(req.getParameter("active")),
                toInt(req.getParameter("minExpected")),
                toInt(req.getParameter("maxExpected")),
                req.getParameter("deadline"),
                toInt(req.getParameter("warningMinutesBeforeDeadline")),
                req.getParameter("windowsSpec"),
                req.getParameter("weekdays"),
                req.getParameter("monthDays"),
                req.getParameter("specificDates"),
                toBoolean(req.getParameter("useHistoricalBaseline")),
                toInt(req.getParameter("historicalDays")),
                toInt(req.getParameter("minPercentOfAverage"))
        );
        writeOk(resp, "{\"message\":\"Regel sparad\"}");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doPost(req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        RuleService service = (RuleService) getServletContext().getAttribute(AppBootstrap.RULE_SERVICE_KEY);
        service.delete(req.getParameter("id"));
        writeOk(resp, "{\"message\":\"Regel raderad\"}");
    }

    private String toWindowsSpec(List<MonitoringWindow> windows) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < windows.size(); i++) {
            MonitoringWindow w = windows.get(i);
            if (i > 0) sb.append("\n");
            sb.append(w.getDeadline()).append("|").append(w.getMinExpected()).append("|").append(w.getMaxExpected());
        }
        return sb.toString();
    }

    private void writeOk(HttpServletResponse resp, String body) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(body);
    }

    private int toInt(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        return Integer.parseInt(value.trim());
    }

    private boolean toBoolean(String value) {
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    private boolean toBooleanDefaultTrue(String value) {
        return value == null || value.trim().isEmpty() || "true".equalsIgnoreCase(value.trim());
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }
}
