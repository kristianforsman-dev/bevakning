package se.forsman.bevakning.dev;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import se.forsman.bevakning.admin.AdminConfigService;
import se.forsman.bevakning.admin.RuleImportExportService;
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
        final AppServices services = AppFactory.create();
        final AppConfig config = new AppConfig();

        final CachedDashboardService cachedDashboardService =
                new CachedDashboardService(services.getDashboardService());

        final RefreshScheduler scheduler =
                new RefreshScheduler(cachedDashboardService, config.getInt("app.refresh.seconds", 30));

        try {
            scheduler.start();
        } catch (Exception e) {
            System.err.println("Kunde inte starta refresh-scheduler: " + e.getMessage());
            throw e;
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.setExecutor(Executors.newCachedThreadPool());

        server.createContext("/api/dashboard", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    DashboardSnapshot snapshot = cachedDashboardService.getSnapshot();
                    DevHttp.json(exchange, 200, DevJson.dashboard(snapshot, cachedDashboardService.getRefreshedAt()));
                } catch (Exception e) {
                    DevHttp.json(exchange, 500, DevJson.error(e.getMessage()));
                }
            }
        });

        server.createContext("/api/rules", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    String method = exchange.getRequestMethod();

                    if ("GET".equalsIgnoreCase(method)) {
                        DevHttp.json(exchange, 200, DevJson.rules(services.getRuleService().findAll()));
                        return;
                    }

                    Map<String, String> p = DevHttp.params(exchange);

                    if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                        services.getRuleService().saveOrUpdateRule(
                                p.get("id"),
                                p.get("sender"),
                                p.get("receiver"),
                                p.get("msgType"),
                                p.get("description"),
                                p.get("scheduleType"),
                                toBooleanDefaultTrue(p.get("active")),
                                toInt(p.get("minExpected")),
                                toInt(p.get("maxExpected")),
                                p.get("deadline"),
                                toInt(p.get("warningMinutesBeforeDeadline")),
                                p.get("windowsSpec"),
                                p.get("weekdays"),
                                p.get("monthDays"),
                                p.get("specificDates"),
                                toBoolean(p.get("useHistoricalBaseline")),
                                toInt(p.get("historicalDays")),
                                toInt(p.get("minPercentOfAverage"))
                        );
                        cachedDashboardService.refresh();
                        DevHttp.json(exchange, 200, DevJson.okMessage("Regel sparad"));
                        return;
                    }

                    if ("DELETE".equalsIgnoreCase(method)) {
                        services.getRuleService().delete(p.get("id"));
                        cachedDashboardService.refresh();
                        DevHttp.json(exchange, 200, DevJson.okMessage("Regel raderad"));
                        return;
                    }

                    DevHttp.json(exchange, 405, DevJson.error("Method not allowed"));
                } catch (Exception e) {
                    DevHttp.json(exchange, 500, DevJson.error(e.getMessage()));
                }
            }
        });

        server.createContext("/api/acknowledge", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        DevHttp.json(exchange, 405, DevJson.error("Method not allowed"));
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
                } catch (Exception e) {
                    DevHttp.json(exchange, 500, DevJson.error(e.getMessage()));
                }
            }
        });

        server.createContext("/api/history", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    DevHttp.json(exchange, 200, DevJson.history(services.getAcknowledgementService().findAllHistory()));
                } catch (Exception e) {
                    DevHttp.json(exchange, 500, DevJson.error(e.getMessage()));
                }
            }
        });

        server.createContext("/api/health", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                DevHttp.json(exchange, 200, DevJson.health(services.getMode(), services.getSourceDescription()));
            }
        });

        server.createContext("/api/admin/config", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    AdminConfigService adminConfigService = new AdminConfigService(config, services);
                    DevHttp.json(exchange, 200, adminConfigService.toJson());
                } catch (Exception e) {
                    DevHttp.json(exchange, 500, DevJson.error(e.getMessage()));
                }
            }
        });

        server.createContext("/api/admin/rules/export", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    RuleImportExportService service = new RuleImportExportService(config);
                    DevHttp.json(exchange, 200, service.exportRulesJson());
                } catch (Exception e) {
                    DevHttp.json(exchange, 500, DevJson.error(e.getMessage()));
                }
            }
        });

        server.createContext("/api/admin/rules/import", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                        DevHttp.json(exchange, 405, DevJson.error("Method not allowed"));
                        return;
                    }

                    String body = DevHttp.readBody(exchange);
                    RuleImportExportService service = new RuleImportExportService(config);
                    service.importRulesJson(body);
                    cachedDashboardService.refresh();
                    DevHttp.json(exchange, 200, DevJson.okMessage("Regler importerade"));
                } catch (Exception e) {
                    DevHttp.json(exchange, 500, DevJson.error(e.getMessage()));
                }
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
        System.out.println("Mode: " + services.getMode());
        System.out.println("Source: " + services.getSourceDescription());
        server.start();
    }

    private static int toInt(String value) {
        if (value == null || value.trim().isEmpty()) return 0;
        return Integer.parseInt(value.trim());
    }

    private static boolean toBoolean(String value) {
        return value != null && "true".equalsIgnoreCase(value.trim());
    }

    private static boolean toBooleanDefaultTrue(String value) {
        return value == null || value.trim().isEmpty() || "true".equalsIgnoreCase(value.trim());
    }
}
