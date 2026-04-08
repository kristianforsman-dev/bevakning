package se.forsman.bevakning.dev;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class StaticFileHandler implements HttpHandler {
    private final File baseDir;

    public StaticFileHandler(File baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path)) {
            path = "/index.html";
        }

        File file = new File(baseDir, path);
        if (!file.getCanonicalPath().startsWith(baseDir.getCanonicalPath()) || !file.exists() || file.isDirectory()) {
            DevHttp.text(exchange, 404, "text/plain; charset=UTF-8", "Not Found".getBytes("UTF-8"));
            return;
        }

        String contentType = contentType(file.getName());
        DevHttp.text(exchange, 200, contentType, Files.readAllBytes(file.toPath()));
    }

    private String contentType(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lower.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        return "application/octet-stream";
    }
}
