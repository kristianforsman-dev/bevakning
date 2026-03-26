package se.forsman.bevakning.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonUtils {
    private JsonUtils() {
    }

    public static Object parseJsonResource(String resourcePath) {
        return parseJson(readResource(resourcePath));
    }

    public static Object parseJson(String json) {
        if (json == null) {
            throw new IllegalArgumentException("JSON får inte vara null");
        }
        Parser parser = new Parser(json);
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (!parser.isEnd()) {
            throw new IllegalStateException("Ogiltig JSON, oväntat innehåll på position " + parser.pos);
        }
        return value;
    }

    private static String readResource(String resourcePath) {
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new IllegalStateException("Kunde inte hitta resource: " + resourcePath);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (IOException e) {
            throw new IllegalStateException("Kunde inte läsa resource: " + resourcePath, e);
        }
        return sb.toString();
    }

    private static final class Parser {
        private final String text;
        private final int len;
        private int pos;

        private Parser(String text) {
            this.text = text;
            this.len = text.length();
            this.pos = 0;
        }

        private Object parseValue() {
            skipWhitespace();
            if (isEnd()) {
                throw new IllegalStateException("Ogiltig JSON: tomt innehåll");
            }

            char c = current();
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't') return parseTrue();
            if (c == 'f') return parseFalse();
            if (c == 'n') return parseNull();
            if (c == '-' || isDigit(c)) return parseNumber();

            throw new IllegalStateException("Ogiltig JSON: oväntat tecken '" + c + "' på position " + pos);
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
                Object value = parseValue();
                list.add(value);
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

            while (!isEnd()) {
                char c = current();
                pos++;

                if (c == '"') {
                    return sb.toString();
                }

                if (c == '\\') {
                    if (isEnd()) {
                        throw new IllegalStateException("Ogiltig JSON-sträng: avslutas efter escape");
                    }
                    char e = current();
                    pos++;

                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            sb.append(parseUnicodeEscape());
                            break;
                        default:
                            throw new IllegalStateException("Ogiltig escape-sekvens \\" + e + " på position " + (pos - 1));
                    }
                } else {
                    sb.append(c);
                }
            }

            throw new IllegalStateException("Ogiltig JSON-sträng: saknar avslutande citationstecken");
        }

        private char parseUnicodeEscape() {
            if (pos + 4 > len) {
                throw new IllegalStateException("Ogiltig unicode-escape på position " + pos);
            }
            String hex = text.substring(pos, pos + 4);
            pos += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Ogiltig unicode-escape: \\u" + hex);
            }
        }

        private Boolean parseTrue() {
            expectLiteral("true");
            return Boolean.TRUE;
        }

        private Boolean parseFalse() {
            expectLiteral("false");
            return Boolean.FALSE;
        }

        private Object parseNull() {
            expectLiteral("null");
            return null;
        }

        private Number parseNumber() {
            int start = pos;

            if (peek('-')) {
                pos++;
            }

            if (isEnd()) {
                throw new IllegalStateException("Ogiltigt nummer på position " + start);
            }

            if (peek('0')) {
                pos++;
            } else {
                if (!isDigit(current())) {
                    throw new IllegalStateException("Ogiltigt nummer på position " + start);
                }
                while (!isEnd() && isDigit(current())) {
                    pos++;
                }
            }

            boolean isDecimal = false;

            if (!isEnd() && peek('.')) {
                isDecimal = true;
                pos++;
                if (isEnd() || !isDigit(current())) {
                    throw new IllegalStateException("Ogiltigt decimalnummer på position " + start);
                }
                while (!isEnd() && isDigit(current())) {
                    pos++;
                }
            }

            if (!isEnd() && (peek('e') || peek('E'))) {
                isDecimal = true;
                pos++;
                if (!isEnd() && (peek('+') || peek('-'))) {
                    pos++;
                }
                if (isEnd() || !isDigit(current())) {
                    throw new IllegalStateException("Ogiltig exponent i nummer på position " + start);
                }
                while (!isEnd() && isDigit(current())) {
                    pos++;
                }
            }

            String number = text.substring(start, pos);
            try {
                if (isDecimal) {
                    return Double.valueOf(number);
                }
                long longValue = Long.parseLong(number);
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return Integer.valueOf((int) longValue);
                }
                return Long.valueOf(longValue);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Ogiltigt nummer: " + number, e);
            }
        }

        private void expect(char expected) {
            skipWhitespace();
            if (isEnd() || current() != expected) {
                throw new IllegalStateException("Förväntade '" + expected + "' på position " + pos);
            }
            pos++;
        }

        private void expectLiteral(String literal) {
            skipWhitespace();
            if (pos + literal.length() > len || !text.substring(pos, pos + literal.length()).equals(literal)) {
                throw new IllegalStateException("Förväntade \"" + literal + "\" på position " + pos);
            }
            pos += literal.length();
        }

        private void skipWhitespace() {
            while (!isEnd()) {
                char c = current();
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private boolean peek(char expected) {
            return !isEnd() && current() == expected;
        }

        private char current() {
            return text.charAt(pos);
        }

        private boolean isEnd() {
            return pos >= len;
        }

        private boolean isDigit(char c) {
            return c >= '0' && c <= '9';
        }
    }
}
