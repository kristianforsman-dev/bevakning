package se.forsman.bevakning.dev;

import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class DevHttp {
    private DevHttp() {
    }

    public static void json(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.flush();
        os.close();
    }

    public static void text(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length);
        OutputStream os = exchange.getResponseBody();
        os.write(body);
        os.flush();
        os.close();
    }

    public static Map<String, String> params(HttpExchange exchange) throws IOException {
        Map<String, String> result = new HashMap<String, String>();

        String query = exchange.getRequestURI().getRawQuery();
        if (query != null && !query.trim().isEmpty()) {
            parseInto(query, result);
        }

        InputStream in = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        String body = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        if (!body.trim().isEmpty()) {
            parseInto(body, result);
        }

        return result;
    }

    private static void parseInto(String input, Map<String, String> result) throws UnsupportedEncodingException {
        String[] pairs = input.split("&");
        for (String pair : pairs) {
            if (pair == null || pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            String key = decode(kv[0]);
            String value = kv.length > 1 ? decode(kv[1]) : "";
            result.put(key, value);
        }
    }

    private static String decode(String value) throws UnsupportedEncodingException {
        return URLDecoder.decode(value, "UTF-8");
    }
}
