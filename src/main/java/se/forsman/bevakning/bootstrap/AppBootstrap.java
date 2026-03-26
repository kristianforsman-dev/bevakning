package se.forsman.bevakning.bootstrap;

import se.forsman.bevakning.app.AppFactory;
import se.forsman.bevakning.app.AppServices;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppBootstrap implements ServletContextListener {
    public static final String DASHBOARD_SERVICE_KEY = "dashboardService";
    public static final String RULE_SERVICE_KEY = "ruleService";
    public static final String ACK_SERVICE_KEY = "ackService";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        AppServices services = AppFactory.create();
        ServletContext context = sce.getServletContext();
        context.setAttribute(DASHBOARD_SERVICE_KEY, services.getDashboardService());
        context.setAttribute(RULE_SERVICE_KEY, services.getRuleService());
        context.setAttribute(ACK_SERVICE_KEY, services.getAcknowledgementService());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
