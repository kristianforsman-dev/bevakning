package se.forsman.bevakning.util;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

public final class AppLogger {
    private static boolean initialized = false;

    public static Logger getLogger(Class<?> type) {
        if (!initialized) {
            try {
                new java.io.File("logs").mkdirs();
                FileHandler handler = new FileHandler("logs/app.log", true);
                Logger root = Logger.getLogger("");
                root.addHandler(handler);
                initialized = true;
            } catch (IOException e) {
                System.err.println("Logger init fel: " + e.getMessage());
            }
        }
        return Logger.getLogger(type.getName());
    }
}
