package se.forsman.bevakning.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtils {

    private JsonUtils() {
    }

    public static String readResource(String resourcePath) {
        String normalized = normalize(resourcePath);

        String fromClasspath = readFromClasspath(normalized);
        if (fromClasspath != null) {
            return fromClasspath;
        }

        String fromSourceResources = readFromSourceResources(normalized);
        if (fromSourceResources != null) {
            return fromSourceResources;
        }

        throw new IllegalStateException("Kunde inte hitta resource: " + resourcePath);
    }

    public static Object parseJsonResource(String resourcePath) {
        return parseJson(readResource(resourcePath));
    }

    public static Object parseJson(String json) {
        if (json == null) {
            return null;
        }

        String cleaned = stripBom(json).trim();
        if (cleaned.isEmpty()) {
            return null;
        }

        Object parsed = new Parser(cleaned).parse();
        return unwrapNestedJsonString(parsed, 0);
    }

    private static Object unwrapNestedJsonString(Object value, int depth) {
        if (!(value instanceof String) || depth > 5) {
            return value;
        }

        String s = stripBom(((String) value).trim());
        if (s.isEmpty()) {
            return s;
        }

        boolean looksLikeJson =
                (s.startsWith("{") && s.endsWith("}")) ||
                (s.startsWith("[") && s.endsWith("]"));

        if (!looksLikeJson) {
            return value;
        }

        Object reparsed = new Parser(s).parse();
        return unwrapNestedJsonString(reparsed, depth + 1);
    }

    private static String stripBom(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }

    private static String readFromClasspath(String resourcePath) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = JsonUtils.class.getClassLoader();
        }

        InputStream in = cl.getResourceAsStream(resourcePath);
        if (in == null) {
            return null;
        }

        try {
            return readAll(in);
        } catch (IOException e) {
            throw new IllegalStateException("Kunde inte läsa resource från classpath: " + resourcePath, e);
        }
    }

    private static String readFromSourceResources(String resourcePath) {
        Path path = Paths.get("src", "main", "resources").resolve(resourcePath);
        if (!Files.exists(path)) {
            return null;
        }

        try {
            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Kunde inte läsa resource från filsystem: " + path, e);
        }
    }

    private static String readAll(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }

        return sb.toString();
    }

    private static String normalize(String resourcePath) {
        String value = resourcePath == null ? "" : resourcePath.trim();
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        return value;
    }

    private static final class Parser {
        private final String json;
        private int pos;

        private Parser(String json) {
            this.json = json;
            this.pos = 0;
        }

        private Object parse() {
            skipWhitespace();
            Object value = parseValue();
            skipWhitespace();
            return value;
        }

        private Object parseValue() {
            skipWhitespace();
            if (pos >= json.length()) {
                throw error("Oväntat slut på JSON");
            }

            char ch = json.charAt(pos);

            if (ch == '{') return parseObject();
            if (ch == '[') return parseArray();
            if (ch == '"') return parseString();
            if (ch == 't') return parseTrue();
            if (ch == 'f') return parseFalse();
            if (ch == 'n') return parseNull();
            if (ch == '-' || Character.isDigit(ch)) return parseNumber();

            throw error("Ogiltigt JSON-värde vid position " + pos);
        }

        private Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();

            Map<String, Object> map = new LinkedHashMap<String, Object>();
            if (peek('}')) {
                expect('}');
                return map;
            }

            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();

                if (peek('}')) {
                    expect('}');
                    break;
                }

                expect(',');
            }

            return map;
        }

        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();

            List<Object> list = new ArrayList<Object>();
            if (peek(']')) {
                expect(']');
                return list;
            }

            while (true) {
                skipWhitespace();
                list.add(parseValue());
                skipWhitespace();

                if (peek(']')) {
                    expect(']');
                    break;
                }

                expect(',');
            }

            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();

            while (pos < json.length()) {
                char ch = json.charAt(pos++);

                if (ch == '"') {
                    return sb.toString();
                }

                if (ch == '\\') {
                    if (pos >= json.length()) {
                        throw error("Ogiltig escape i sträng");
                    }

                    char esc = json.charAt(pos++);
                    switch (esc) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u': sb.append(parseUnicode()); break;
                        default: throw error("Okänd escape-sekvens: \\" + esc);
                    }
                } else {
                    sb.append(ch);
                }
            }

            throw error("Oavslutad sträng");
        }

        private char parseUnicode() {
            if (pos + 4 > json.length()) {
                throw error("Ogiltig unicode escape");
            }

            String hex = json.substring(pos, pos + 4);
            pos += 4;

            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                throw error("Ogiltig unicode escape: " + hex);
            }
        }

        private Boolean parseTrue() {
            expectWord("true");
            return Boolean.TRUE;
        }

        private Boolean parseFalse() {
            expectWord("false");
            return Boolean.FALSE;
        }

        private Object parseNull() {
            expectWord("null");
            return null;
        }

        private Number parseNumber() {
            int start = pos;

            if (json.charAt(pos) == '-') {
                pos++;
            }

            while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                pos++;
            }

            boolean isDecimal = false;

            if (pos < json.length() && json.charAt(pos) == '.') {
                isDecimal = true;
                pos++;
                while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                    pos++;
                }
            }

            if (pos < json.length()) {
                char ch = json.charAt(pos);
                if (ch == 'e' || ch == 'E') {
                    isDecimal = true;
                    pos++;
                    if (pos < json.length() && (json.charAt(pos) == '+' || json.charAt(pos) == '-')) {
                        pos++;
                    }
                    while (pos < json.length() && Character.isDigit(json.charAt(pos))) {
                        pos++;
                    }
                }
            }

            String value = json.substring(start, pos);

            try {
                if (isDecimal) {
                    return Double.valueOf(value);
                }
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                throw error("Ogiltigt tal: " + value);
            }
        }

        private void skipWhitespace() {
            while (pos < json.length()) {
                char ch = json.charAt(pos);
                if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private boolean peek(char expected) {
            return pos < json.length() && json.charAt(pos) == expected;
        }

        private void expect(char expected) {
            if (pos >= json.length() || json.charAt(pos) != expected) {
                throw error("Förväntade '" + expected + "' vid position " + pos);
            }
            pos++;
        }

        private void expectWord(String word) {
            if (pos + word.length() > json.length()) {
                throw error("Förväntade \"" + word + "\"");
            }

            String actual = json.substring(pos, pos + word.length());
            if (!word.equals(actual)) {
                throw error("Förväntade \"" + word + "\" men fick \"" + actual + "\"");
            }

            pos += word.length();
        }

        private IllegalStateException error(String message) {
            return new IllegalStateException(message);
        }
    }
}
