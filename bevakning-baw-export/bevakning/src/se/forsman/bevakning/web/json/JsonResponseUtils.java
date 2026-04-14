package se.forsman.bevakning.web.json;

public final class JsonResponseUtils {
    private JsonResponseUtils() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }

    public static String message(String message) {
        return "{\"message\":\"" + escape(message) + "\"}";
    }

    public static String error(String message) {
        return "{\"status\":\"ERROR\",\"message\":\"" + escape(message) + "\"}";
    }

    public static String health(String status, String mode, String source) {
        return "{"
                + "\"status\":\"" + escape(status) + "\","
                + "\"mode\":\"" + escape(mode) + "\","
                + "\"source\":\"" + escape(source) + "\""
                + "}";
    }
}
