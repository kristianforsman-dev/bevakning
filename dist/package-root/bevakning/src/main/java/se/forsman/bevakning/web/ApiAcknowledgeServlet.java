package se.forsman.bevakning.web;

import se.forsman.bevakning.bootstrap.AppBootstrap;
import se.forsman.bevakning.service.AcknowledgementService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;

@WebServlet("/api/acknowledge")
public class ApiAcknowledgeServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        AcknowledgementService service =
                (AcknowledgementService) getServletContext().getAttribute(AppBootstrap.ACK_SERVICE_KEY);

        service.acknowledge(
                req.getParameter("ruleId"),
                req.getParameter("status"),
                req.getParameter("comment"),
                req.getParameter("acknowledgedBy")
        );

        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write("{\"message\":\"Kvittering sparad\"}");
    }
}
