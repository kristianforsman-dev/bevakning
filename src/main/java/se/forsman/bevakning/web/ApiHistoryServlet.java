package se.forsman.bevakning.web;

import se.forsman.bevakning.bootstrap.AppBootstrap;
import se.forsman.bevakning.domain.AlertHistoryEntry;
import se.forsman.bevakning.service.AcknowledgementService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet("/api/history")
public class ApiHistoryServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AcknowledgementService service =
                (AcknowledgementService) getServletContext().getAttribute(AppBootstrap.ACK_SERVICE_KEY);

        List<AlertHistoryEntry> rows = service.findAllHistory();

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < rows.size(); i++) {
            AlertHistoryEntry row = rows.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{");
            sb.append("\"id\":\"").append(escape(row.getId())).append("\",");
            sb.append("\"ruleId\":\"").append(escape(row.getRuleId())).append("\",");
            sb.append("\"occurrenceKey\":\"").append(escape(row.getOccurrenceKey())).append("\",");
            sb.append("\"eventType\":\"").append(escape(row.getEventType())).append("\",");
            sb.append("\"status\":\"").append(escape(row.getStatus())).append("\",");
            sb.append("\"message\":\"").append(escape(row.getMessage())).append("\",");
            sb.append("\"createdAt\":\"").append(escape(row.getCreatedAt())).append("\",");
            sb.append("\"createdBy\":\"").append(escape(row.getCreatedBy())).append("\"");
            sb.append("}");
        }
        sb.append("]");

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(sb.toString());
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
