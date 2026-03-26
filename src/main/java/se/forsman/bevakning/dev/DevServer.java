package se.forsman.bevakning.dev;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import se.forsman.bevakning.app.AppFactory;
import se.forsman.bevakning.app.AppServices;
import se.forsman.bevakning.config.AppConfig;
import se.forsman.bevakning.domain.DashboardSnapshot;
import se.forsman.bevakning.service.CachedDashboardService;
import se.forsman.bevakning.service.RefreshScheduler;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

public class DevServer {
    public static void main(String[] args) throws Exception {
        AppServices services = AppFactory.create();
        AppConfig config = new AppConfig();

        final CachedDashboardService cachedDashboardService =
                new CachedDashboardService(services.getDashboardService());

        final RefreshScheduler scheduler =
                new RefreshScheduler(cachedDashboardService, config.getInt("app.refresh.seconds", 30));
        scheduler.start();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/api/dashboard", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                DashboardSnapshot snapshot = cachedDashboardService.getSnapshot();
                DevHttp.json(exchange, 200, DevJson.dashboard(snapshot, cachedDashboardService.getRefreshedAt()));
            }
        });

        server.createContext("/api/rules", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String method = exchange.getRequestMethod();

                if ("GET".equalsIgnoreCase(method)) {
                    DevHttp.json(exchange, 200, DevJson.rules(services.getRuleService().findAll()));
                    return;
                }

                Map<String, String> p = DevHttp.params(exchange);

                if ("POST".equalsIgnoreCase(method)) {
                    services.getRuleService().createDailyRule(
                            p.get("sender"),
                            p.get("receiver"),
                            p.get("msgType"),
                            p.get("description"),
                            toInt(p.get("minExpected")),
                            toInt(p.get("maxExpected")),
                            p.get("deadline"),
                            toInt(p.get("warningMinutesBeforeDeadline"))
                    );
                    cachedDashboardService.refresh();
                    DevHttp.json(exchange, 200, DevJson.okMessage("Regel skapad"));
                    return;
                }

                if ("PUT".equalsIgnoreCase(method)) {
                    services.getRuleService().updateDailyRule(
                            p.get("id"),
                            p.get("sender"),
                            p.get("receiver"),
                            p.get("msgType"),
                            p.get("description"),
                            toInt(p.get("minExpected")),
                            toInt(p.get("maxExpected")),
                            p.get("deadline"),
                            toInt(p.get("warningMinutesBeforeDeadline")),
                            toBoolean(p.get("active"))
                    );
                    cachedDashboardService.refresh();
                    DevHttp.json(exchange, 200, DevJson.okMessage("Regel uppdaterad"));
                    return;
                }

                if ("DELETE".equalsIgnoreCase(method)) {
                    services.getRuleService().delete(p.get("id"));
                    cachedDashboardService.refresh();
                    DevHttp.json(exchange, 200, DevJson.okMessage("Regel raderad"));
                    return;
                }

                DevHttp.json(exchange, 405, "{\"message\":\"Method not allowed\"}");
            }
        });

        server.createContext("/api/acknowledge", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    DevHttp.json(exchange, 405, "{\"message\":\"Method not allowed\"}");
                    return;
                }

                Map<String, String> p = DevHttp.params(exchange);
                services.getAcknowledgementService().acknowledge(
                        p.get("ruleId"),
                        p.get("status"),
                        p.get("comment"),
                        p.get("acknowledgedBy")
                );
                DevHttp.json(exchange, 200, DevJson.okMessage("Kvittering sparad"));
            }
        });

        server.createContext("/api/history", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                DevHttp.json(exchange, 200, DevJson.history(services.getAcknowledgementService().findAllHistory()));
            }
        });

        server.createContext("/", new StaticFileHandler(new File("src/main/devui")));

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                scheduler.stop();
            }
        }));

        System.out.println("Dev server startad på http://localhost:8080");
        server.start();
    }

    private static int toInt(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        return Integer.parseInt(value.trim());
    }

    private static boolean toBoolean(String value) {
        return value != null && "true".equalsIgnoreCase(value.trim());
    }
}
