package se.forsman.bevakning.web.json;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class ServletJsonWriter {
    private ServletJsonWriter() {
    }

    public static void writeJson(HttpServletResponse resp, String body) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(body);
    }
}
