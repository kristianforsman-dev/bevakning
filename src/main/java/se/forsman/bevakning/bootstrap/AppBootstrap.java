package se.forsman.bevakning.bootstrap;

import se.forsman.bevakning.app.AppFactory;
import se.forsman.bevakning.app.AppServices;
import se.forsman.bevakning.config.AppConfig;
import se.forsman.bevakning.service.CachedDashboardService;
import se.forsman.bevakning.service.RefreshScheduler;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppBootstrap implements ServletContextListener {
    public static final String APP_SERVICES_KEY = "appServices";
    public static final String DASHBOARD_SERVICE_KEY = "dashboardService";
    public static final String RULE_SERVICE_KEY = "ruleService";
    public static final String ACK_SERVICE_KEY = "ackService";
    public static final String REFRESH_SCHEDULER_KEY = "refreshScheduler";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        AppServices services = AppFactory.create();
        AppConfig config = new AppConfig();
        ServletContext context = sce.getServletContext();

        CachedDashboardService cachedDashboardService =
                new CachedDashboardService(services.getDashboardService());

        RefreshScheduler refreshScheduler =
                new RefreshScheduler(
                        cachedDashboardService,
                        config.getInt("db2.query.poll.seconds", config.getInt("app.refresh.seconds", 30))
                );

        refreshScheduler.start();

        context.setAttribute(APP_SERVICES_KEY, services);
        context.setAttribute(DASHBOARD_SERVICE_KEY, cachedDashboardService);
        context.setAttribute(RULE_SERVICE_KEY, services.getRuleService());
        context.setAttribute(ACK_SERVICE_KEY, services.getAcknowledgementService());
        context.setAttribute(REFRESH_SCHEDULER_KEY, refreshScheduler);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Object scheduler = sce.getServletContext().getAttribute(REFRESH_SCHEDULER_KEY);
        if (scheduler instanceof RefreshScheduler) {
            ((RefreshScheduler) scheduler).stop();
        }
    }
}
